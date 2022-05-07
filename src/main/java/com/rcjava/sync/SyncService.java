package com.rcjava.sync;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketState;
import com.rcjava.client.ChainInfoClient;
import com.rcjava.client.RSubClient;
import com.rcjava.exception.SyncBlockException;
import com.rcjava.protos.Peer;
import com.rcjava.ws.BlockListener;
import com.rcjava.ws.BlockListenerUtil;
import com.rcjava.ws.BlockObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.concurrent.*;

/**
 * 区块同步服务
 *
 * @author zyf
 */
public class SyncService implements BlockObserver {

    private String host;
    private String wsHost;
    private SyncInfo syncInfo;
    private SyncListener syncListener;

    private RSubClient rSubClient;
    private ChainInfoClient cInfoClient_pull;
    private ChainInfoClient cInfoClient_sub;
    private BlockListener blkListener;

    private ThreadFactory pullServiceFactory = new ThreadFactoryBuilder().setNameFormat("blockPull-pool-%d").build();
    private ScheduledExecutorService pullService;
    private ThreadFactory wsServiceFactory = new ThreadFactoryBuilder().setNameFormat("wsMonitor-pool-%d").build();
    private ScheduledExecutorService wsService;

    // socket是否已经创建过的标志位
    private volatile boolean isWScreated = false;

    // 正在pull
    private volatile boolean pullSyncing = false;
    // 正在sub
    private volatile boolean subSyncing = false;

    // 停止pull
    private volatile boolean stopPull = false;


    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private SyncService(Builder builder) {
        host = builder.host;
        wsHost = builder.wsHost;
        syncInfo = builder.syncInfo;
        syncListener = builder.syncListener;
    }

    public static Builder newBuilder() {
        return new Builder();
    }


    /**
     * 开始同步服务
     */
    public void start() {

        logger.info("已触发start，正在启动SyncService...");

        this.stopPull = false;
        this.isWScreated = false;
        this.pullSyncing = false;
        this.subSyncing = false;

        this.wsHost = "".equals(wsHost) ? host : wsHost;

        // 获取block监听，使用 host 获取一个监听，每个 host 对应一个监听
        blkListener = BlockListenerUtil.getListener(wsHost);
        // event 监听，并回调给具体的实现类
        blkListener.registerBlkObserver(this);

        // pull用，使用 cInfoClient_pull 来定时 pull 区块
        cInfoClient_pull = new ChainInfoClient(host);
        // sub用
        rSubClient = new RSubClient(wsHost, blkListener);
        // sub 与 pull 结合时，使用 cInfoClient_sub 来 pull 区块
        cInfoClient_sub = wsHost.equals(host) ? cInfoClient_pull : new ChainInfoClient(wsHost);

        wsService = Executors.newSingleThreadScheduledExecutor(wsServiceFactory);
        // 首先打开订阅，每隔60s检查一次socket状态
        wsService.scheduleAtFixedRate(this::startSub, 5, 60, TimeUnit.SECONDS);
        pullService = Executors.newSingleThreadScheduledExecutor(pullServiceFactory);
        // 打开定时pull，固定间隔时间20s，检查是否需要pull
        pullService.scheduleWithFixedDelay(this::startPull, 15, 20, TimeUnit.SECONDS);

    }

    /**
     * 停止同步服务，非立即停止
     */
    public void stop() {

        logger.info("已触发stop，正在终止SyncService...");

        // 跳出execPull中可能正在进行的while循环
        this.stopPull = true;

        BlockListenerUtil.removeListener(wsHost);
        blkListener.removeBlkObserver(this);

        // 断开socket连接
        rSubClient.disconnect();

        try {
            // 停掉socket监测服务
            wsService.shutdown();
            // 停掉pull服务
            pullService.shutdown();
            boolean stopWsService = wsService.awaitTermination(5, TimeUnit.SECONDS);
            boolean stopPullService = pullService.awaitTermination(5, TimeUnit.SECONDS);
            logger.info("stop wsService {}, stop pullService {}", stopWsService, stopPullService);
        } catch (InterruptedException e) {
            logger.error("shutdown wsService or pullService occurs error, errorMsg is {}", e.getMessage(), e);
        }

    }

    /**
     * 谨慎使用，要确认syncInfo是否已经被更新，客户端可调用setSyncInfo，或直接默认
     */
    public void restart() {
        logger.info("已触发restart，正在重启SyncService..., be careful, please confirm the syncInfo has been updated");
        stop();
        start();
    }

    /**
     * 开始定时pull，使用pull的方式拉取区块
     */
    private void startPull() {
        // 防止极端情况下websocket断了
        if (subSyncing && rSubClient.getWs().isOpen()) {
            return;
        }
        pullSyncing = true;
        execPull(cInfoClient_pull);
        // 置为false是为了在websocket监测到出块事件时，更快的响应(进入onMessage逻辑)，如果不置为false，可能最长需要等待20s（pullService的设置）
        pullSyncing = false;
    }

    /**
     * 使用pull的方式拉取区块
     */
    private synchronized void execPull(ChainInfoClient cInfoClient) {
        logger.info("执行同步，开始pull {}...，execPull", cInfoClient.getHost());
        try {
            long localHeight = syncInfo.getLocalHeight();
            long remoteHeight = cInfoClient.getChainInfo().getHeight();
            while (localHeight < remoteHeight) {
                long tempHeight = localHeight + 1;
                Peer.Block block = cInfoClient.getBlockStreamByHeight(tempHeight);
                // block的hash是否可以衔接
                if (block.getPreviousBlockHash().toStringUtf8().equals(syncInfo.getLocBlkHash()) || block.getPreviousBlockHash().isEmpty()) {
                    logger.info("pullBlock, localHeight：{}, localBlockHash：{}, pullHeight：{}, pullBlockHash：{}",
                            syncInfo.getLocalHeight(), syncInfo.getLocBlkHash(), block.getHeight(), block.getHashOfBlock().toStringUtf8());
                    syncListener.onSuccess(block);
                    logger.info("save block {} successfully", tempHeight);
                    syncInfo.setLocalHeight(tempHeight);
                    syncInfo.setLocBlkHash(block.getHashOfBlock().toStringUtf8());
                    localHeight = tempHeight;
                } else {
                    logger.error("pull: 块Hash衔接不上, localHeight：{}, localBlockHash：{}, pullHeight：{}, pullBlockHash：{}",
                            syncInfo.getLocalHeight(), syncInfo.getLocBlkHash(), block.getHeight(), block.getHashOfBlock().toStringUtf8());
                    syncListener.onError(new SyncBlockException(String.format("块Hash衔接不上, localHeight：%s, localBlockHash：%s, pullHeight：%s, pullBlockHash：%s, please restart syncService",
                            syncInfo.getLocalHeight(), syncInfo.getLocBlkHash(), block.getHeight(), block.getHashOfBlock().toStringUtf8())));
                    break;
                }
                if (stopPull) {
                    break;
                }
            }
            if (stopPull) {
                logger.info("已触发stop，停止pull服务...");
            } else {
                logger.info("localHeight: {}, remoteHeight: {}, pull结束或者无需pull, 切换pull为sub, 开始sub...", localHeight, remoteHeight);
            }
        } catch (SyncBlockException syncEx) {
            logger.error("-----订阅端发生区块处理异常-----", syncEx);
        } catch (Exception e) {
            logger.error("errorMsg: {}", e.getMessage(), e);
            syncListener.onError(new SyncBlockException(String.format("startSub error, connect webSocket error, errorMsg: %s, please restart syncService", e.getMessage()), e));
        }
    }

    /**
     * 开始订阅
     */
    private void startSub() {
        try {
            // 已经连接过了
            if (isWScreated) {
                logger.info("rSubClient's state is {}.", rSubClient.getWs().getState().name());
                if (!rSubClient.isopen() || rSubClient.getWs().getState() == WebSocketState.CLOSED) {
                    logger.info("rSubClient's state is {}, rSubClient 正在重新连接 {}...", rSubClient.getWs().getState().name(), rSubClient.getHost());
                    // 如果连接断掉，要将pullSyncing与subSyncing分别置为false，这样startPull()可以继续工作
                    pullSyncing = false;
                    subSyncing = false;
                    // 非open状态，重连一下
                    rSubClient.reconnect();
                }
                // 60s---ping
                // rSubClient.getWs().sendPing("keep alive");
            } else {
                // 连接ws，开启订阅
                rSubClient.connect();
                isWScreated = true;
                logger.info("rSubClient 正在连接 {}...", rSubClient.getHost());
            }
        } catch (WebSocketException | IOException e) {
            logger.error("startSub error, rSubClient connect server error, errorMsg: {}", e.getMessage(), e.getCause());
            syncListener.onError(new SyncBlockException(String.format("startSub error, rSubClient connect server error, errorMsg: %s, please restart syncService", e.getMessage()), e));
        }
    }

    /**
     * 通过订阅服务，得到的区块
     *
     * @param block 回调的块数据
     */
    @Override
    public void onMessage(Peer.Block block) {
        if (pullSyncing) {
            return;
        }
        subSyncing = true;
        synchronized (this) {
            logger.info("执行同步，开始sub {}...，execSub", rSubClient.getHost());
            if (block.getPreviousBlockHash().toStringUtf8().equals(syncInfo.getLocBlkHash())) {
                logger.info("subBlock，localHeight：{}，localBlockHash：{}，subHeight：{}，subBlockHash：{}",
                        syncInfo.getLocalHeight(), syncInfo.getLocBlkHash(), block.getHeight(), block.getHashOfBlock().toStringUtf8());
                // 加锁是为了保证 onSuccess 线程安全正确
                try {
                    syncListener.onSuccess(block);
                    logger.info("save block {} successfully", block.getHeight());
                    syncInfo.setLocalHeight(syncInfo.getLocalHeight() + 1);
                    syncInfo.setLocBlkHash(block.getHashOfBlock().toStringUtf8());
                } catch (SyncBlockException syncEx) {
                    logger.error("-----订阅端发生区块处理异常-----", syncEx);
                }
            } else {
                logger.info("sub: 块Hash衔接不上，localHeight：{}，localBlockHash：{}，subHeight：{}，subBlockHash：{}",
                        syncInfo.getLocalHeight(), syncInfo.getLocBlkHash(), block.getHeight(), block.getHashOfBlock().toStringUtf8());
                logger.info("切换sub为pull，开始pull...");
                execPull(cInfoClient_sub);
            }
        }
        subSyncing = false;
    }

    public SyncInfo getSyncInfo() {
        return syncInfo;
    }

    public void setSyncInfo(SyncInfo syncInfo) {
        this.syncInfo = syncInfo;
    }

    public static final class Builder {
        private String host;
        private String wsHost = "";
        private SyncInfo syncInfo;
        private SyncListener syncListener;

        private Builder() {
        }

        @Nonnull
        public Builder setHost(@Nonnull String host) {
            this.host = host;
            return this;
        }

        @Nonnull
        public Builder setWsHost(@Nonnull String wsHost) {
            this.wsHost = wsHost;
            return this;
        }

        @Nonnull
        public Builder setSyncInfo(@Nonnull SyncInfo syncInfo) {
            this.syncInfo = syncInfo;
            return this;
        }

        @Nonnull
        public Builder setSyncListener(@Nonnull SyncListener syncListener) {
            this.syncListener = syncListener;
            return this;
        }

        @Nonnull
        public SyncService build() {
            return new SyncService(this);
        }
    }
}
