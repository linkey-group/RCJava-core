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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 同步服务
 *
 * @author zyf
 */
public class SyncService implements BlockObserver {

    private String host;
    private SyncInfo syncInfo;
    private SyncListener syncListener;
    private SyncEndPoint syncEndPoint;

    private RSubClient rSubClient;
    private ChainInfoClient cInfoClient;
    private BlockListener blkListener;

    /**
     * 使用队列保存Block
     */
    private ConcurrentLinkedQueue<Peer.Block> blockQueue = new ConcurrentLinkedQueue<>();

    private ThreadFactory pullServiceFactory = new ThreadFactoryBuilder().setNameFormat("blockPull-pool-%d").build();
    private ScheduledExecutorService pullService = Executors.newSingleThreadScheduledExecutor(pullServiceFactory);
    private ThreadFactory wsServiceFactory = new ThreadFactoryBuilder().setNameFormat("wsMonitor-pool-%d").build();
    private ScheduledExecutorService wsService = Executors.newSingleThreadScheduledExecutor(wsServiceFactory);
    private ThreadFactory blkServiceFactory = new ThreadFactoryBuilder().setNameFormat("blockQueue-pool-%d").build();
    private ScheduledExecutorService blockService = Executors.newSingleThreadScheduledExecutor(blkServiceFactory);

    private volatile boolean isSubing = false;
    private volatile boolean isSyncing = false;

    private volatile boolean stopPull = false;
    private volatile boolean stopPoll = false;


    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private SyncService(Builder builder) {
        host = builder.host;
        syncInfo = builder.syncInfo;
        syncListener = builder.syncListener;
        syncEndPoint = builder.syncEndPoint;
    }

    public static Builder newBuilder() {
        return new Builder();
    }


    /**
     * 开始同步服务
     */
    public void start() {

        // 获取block监听，使用 host 获取一个监听，每个 host 对应一个监听
        blkListener = BlockListenerUtil.getListener(host);
        // event 监听，并回调给具体的实现类
        blkListener.registerBlkObserver(this);

        // pull用
        cInfoClient = new ChainInfoClient(host);
        // sub用
        rSubClient = new RSubClient(host, blkListener);

        // 首先打开订阅，每隔60s检查一次socket状态
        wsService.scheduleAtFixedRate(this::startSub, 1, 60, TimeUnit.SECONDS);

        // 推送block，固定间隔时间10s
        blockService.scheduleWithFixedDelay(this::pollBlock, 2, 10, TimeUnit.SECONDS);

        // 打开定时pull，固定间隔时间20s，检查是否需要pull
        pullService.scheduleWithFixedDelay(this::startPull, 7, 20, TimeUnit.SECONDS);

    }

    /**
     * 停止同步服务，非立即停止
     */
    public void stop() {
        // 断开socket连接
        rSubClient.disconnect();
        // 停掉socket监测服务
        wsService.shutdownNow();
        // 停掉pull服务
        setStopPull();
        pullService.shutdown();
        // 停掉block推送服务
        setStopPoll();
        blockService.shutdown();
    }

    /**
     * 开始定时pull
     */
    private void startPull() {
        if (!isSyncing) {
            logger.info("执行同步，开始pull...，execPull");
            execPull();
        }
    }

    /**
     * 执行pull
     */
    private void execPull() {
        long localHeight = syncInfo.getLocalHeight();
        long remoteHeight = cInfoClient.getChainInfo().getHeight();
        while (localHeight < remoteHeight) {
            long tempHeight = localHeight + 1;
            Peer.Block block = cInfoClient.getBlockStreamByHeight(tempHeight);
            // 由于出现块回滚，可能远端实际高度已经变低，因此可能出现空块
            if (block == null) {
                continue;
            }
            if (block.getPreviousBlockHash().toStringUtf8().equals(syncInfo.getLocBlkHash())) {
                isSyncing = true;
                logger.info("pullBlock，localHeight：{}，localBlockHash：{}，pullHeight：{}，pullBlockHash：{}",
                        syncInfo.getLocalHeight(), syncInfo.getLocBlkHash(), block.getHeight(), block.getHashOfBlock().toStringUtf8());
                syncInfo.setLocalHeight(tempHeight);
                syncInfo.setLocBlkHash(block.getHashOfBlock().toStringUtf8());
                blockQueue.add(block);
                localHeight = tempHeight;
            } else {
                logger.error("pull: 块Hash衔接不上，localHeight：{}，localBlockHash：{}，pullHeight：{}， pullBlockHash：{}",
                        syncInfo.getLocalHeight(), syncInfo.getLocBlkHash(), block.getHeight(), block.getHashOfBlock().toStringUtf8());
                syncListener.onError(new SyncBlockException("RepChain可能回滚块或者块未保存到磁盘上，启动数据修正"),
                        block.getHeight(), block.getPreviousBlockHash().toStringUtf8());
                logger.info("将目前同步到缓存里的数据块，都处理完毕后启动数据修正...");
                // 将块都poll完，然后再进行数据修正，blkQueue线程安全
                pollBlock();
                // 倒着与本地数据库进行遍历比较
                long lTempHeight = localHeight;
                // 需要修正已同步的块
                localHeight = reviseBlock4Roll(lTempHeight);
            }
            if (stopPull) {
                break;
            }
        }
        if (localHeight > remoteHeight) {
            logger.error("本地高度：{} 大于 远端高度：{}，将目前同步到缓存里的数据块，都处理完毕后启动数据修正...", localHeight, remoteHeight);
            syncListener.onError(new SyncBlockException("RepChain可能回滚块或者块未保存到磁盘上，启动数据修正"),
                    remoteHeight, cInfoClient.getBlockByHeight(remoteHeight).getPreviousBlockHash().toStringUtf8());
            // 将块都poll完，然后再进行数据修正，blkQueue线程安全
            pollBlock();
            reviseBlock4Roll(localHeight);
        } else {
            logger.info("localHeight: {} = remoteHeight: {}, pull结束或者无需pull, 切换pull为sub, 开始sub...", localHeight, remoteHeight);
        }
        isSyncing = false;
        if (stopPull) {
            logger.info("已触发stop，停止pull服务...");
        }
    }

    /**
     * 从当前块高度，开始倒着订正因回滚而废弃的块
     *
     * @param currentLocalHeight
     * @return
     */
    private long reviseBlock4Roll(long currentLocalHeight) {
        long localHeight = currentLocalHeight;
        long rTempHeight = cInfoClient.getChainInfo().getHeight();
        for (long height = Math.min(currentLocalHeight, rTempHeight); height > 1; height--) {
            String localBlockHash = syncEndPoint.queryBlockHash(height);
            String remoteBlockHash = cInfoClient.getBlockStreamByHeight(height).getHashOfBlock().toStringUtf8();
            if (localBlockHash.equals(remoteBlockHash)) {
                logger.info("根据业务方提供的接口，检查本地最后的正确块高为: {}，远端当前块高为: {}", height, rTempHeight);
                final List<Peer.Block> blockList = new ArrayList<>();
                // 从本地最后的正确高度，拉取块到tempHeight高度
                for (long i = height + 1; i <= rTempHeight; i++) {
                    blockList.add(cInfoClient.getBlockByHeight(i));
                }
                logger.info("根据业务方提供的接口，更新本地数据库...");
                // 更新本地数据库
                syncEndPoint.update(height, rTempHeight, blockList);
                // 更新syncInfo
                syncInfo.setLocalHeight(rTempHeight);
                syncInfo.setLocBlkHash(blockList.get(blockList.size() - 1).getHashOfBlock().toStringUtf8());
                localHeight = rTempHeight;
                // 跳出修正循环
                break;
            }
        }
        return localHeight;
    }

    /**
     * 开始订阅
     */
    private void startSub() {
        try {
            // 已经连接过了
            if (isSubing) {
                logger.info("rSubClient's state is {}.", rSubClient.getWs().getState().name());
                if (!rSubClient.isopen() || rSubClient.getWs().getState() == WebSocketState.CLOSED) {
                    logger.info("rSubClient's state is {}, rSubClient 正在重新连接...", rSubClient.getWs().getState().name());
                    // 非open状态，重连一下
                    rSubClient.reconnect();
                }
            } else {
                // 连接ws，开启订阅
                rSubClient.connect();
                isSubing = true;
                logger.info("rSubClient 正在连接...");
            }
        } catch (WebSocketException | IOException e) {
            logger.error("startSub error, connect webSocket error, errorMsg: {}", e.getMessage(), e.getCause());
        }
    }

    @Override
    public void onMessage(Peer.Block block) {
        if (block.getPreviousBlockHash().toStringUtf8().equals(syncInfo.getLocBlkHash())) {
            isSyncing = true;
            logger.info("subBlock，localHeight：{}，localBlockHash：{}，subHeight：{}，subBlockHash：{}",
                    syncInfo.getLocalHeight(), syncInfo.getLocBlkHash(), block.getHeight(), block.getHashOfBlock().toStringUtf8());
            syncInfo.setLocalHeight(syncInfo.getLocalHeight() + 1);
            syncInfo.setLocBlkHash(block.getHashOfBlock().toStringUtf8());
            blockQueue.add(block);
        } else {
            isSyncing = false;
            logger.info("sub: 块Hash衔接不上，localHeight：{}，localBlockHash：{}，subHeight：{}，subBlockHash：{}",
                    syncInfo.getLocalHeight(), syncInfo.getLocBlkHash(), block.getHeight(), block.getHashOfBlock().toStringUtf8());
            logger.info("切换sub为pull，开始pull...");
            startPull();
        }
    }

    /**
     * 通知SyncListener，输出block数据
     */
    private void pollBlock() {
        logger.info("PollBlock to SyncListener，开始poll...，blockQueue's size is {}", blockQueue.size());
        onBlock();
    }

    /**
     * BlockQueue pollBlock to SyncListener
     */
    private void onBlock() {
        while (!blockQueue.isEmpty()) {
            syncListener.onBlock(blockQueue.poll());
            if (stopPoll) {
                break;
            }
        }
        if (stopPoll) {
            logger.info("已触发stop，停止poll服务...");
        }
    }

    /**
     * 修改pull标识位
     */
    private void setStopPull() {
        this.stopPull = true;
    }

    /**
     * 修改poll标志位
     */
    private void setStopPoll() {
        this.stopPull = true;
    }

    public static final class Builder {
        private String host;
        private SyncInfo syncInfo;
        private SyncListener syncListener;
        private SyncEndPoint syncEndPoint;

        private Builder() {
        }

        @Nonnull
        public Builder setHost(@Nonnull String host) {
            this.host = host;
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
        public Builder setSyncEndPoint(@Nonnull SyncEndPoint syncEndPoint) {
            this.syncEndPoint = syncEndPoint;
            return this;
        }

        @Nonnull
        public SyncService build() {
            return new SyncService(this);
        }
    }
}
