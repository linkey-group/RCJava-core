package com.rcjava.sync;

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
 * 同步服务
 *
 * @author zyf
 */
public class SyncService implements BlockObserver {

    private String host;
    private SyncInfo syncInfo;
    private SyncListener syncListener;

    private RSubClient rSubClient;
    private ChainInfoClient cInfoClient;
    private BlockListener blkListener;

    // 使用队列保存Block
    private ConcurrentLinkedQueue<Peer.Block> blockQueue = new ConcurrentLinkedQueue<>();

    private ScheduledExecutorService pullService = Executors.newSingleThreadScheduledExecutor();
    private ScheduledExecutorService blockService = Executors.newSingleThreadScheduledExecutor();
    private ScheduledExecutorService wsService = Executors.newSingleThreadScheduledExecutor();

    private volatile boolean isSubing = false;
    private volatile boolean isSyncing = false;
    private volatile boolean isPolling = false;

    private volatile boolean stopPull = false;
    private volatile boolean stopPoll = false;


    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private SyncService(Builder builder) {
        host = builder.host;
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

        // 定时推送block
        blockService.scheduleAtFixedRate(this::pollBlock, 2, 10, TimeUnit.SECONDS);

        // 然后打开定时pull，每隔30s进行检查是否需要pull
        pullService.scheduleAtFixedRate(this::startPull, 7, 20, TimeUnit.SECONDS);

    }

    /**
     * 停止同步服务
     */
    public void stop() {
        // 断开socket连接
        rSubClient.disconnect();
        // 停掉socket监测服务
        wsService.shutdownNow();
        // 停掉pull服务
        setStopPull();
        pullService.shutdownNow();
        // 停掉block推送服务
        setStopPoll();
        blockService.shutdownNow();
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
            if (block.getPreviousBlockHash().toStringUtf8().equals(syncInfo.getLocBlkHash())) {
                isSyncing = true;
                logger.info("pullBlock，localHeight：{}，localBlockHash：{}，pullHeight：{}，pullBlockHash：{}",
                        syncInfo.getLocalHeight(), syncInfo.getLocBlkHash(), block.getHeight(), block.getHashOfBlock().toStringUtf8());
                syncInfo.setLocalHeight(tempHeight);
                syncInfo.setLocBlkHash(block.getHashOfBlock().toStringUtf8());
                blockQueue.add(block);
                localHeight = tempHeight;
            } else {
                syncListener.onError(new SyncBlockException("RepChain可能回滚块，或者块未保存到磁盘上，请纠正数据库等，然后重启服务==> shutdown ==> start"));
                logger.error("pull: 块Hash衔接不上，localHeight：{}，localBlockHash：{}，pullHeight：{}， pullBlockHash：{}",
                        syncInfo.getLocalHeight(), syncInfo.getLocBlkHash(), block.getHeight(), block.getHashOfBlock().toStringUtf8());
                // 跳出本次循环
                break;
            }
            if (stopPull) {
                break;
            }
        }
        isSyncing = false;
        if (stopPull) {
            logger.info("已触发stop，停止pull服务...");
        } else {
            logger.info("localHeight: {} >= remoteHeight: {}, 切换pull为sub, 开始sub...", localHeight, remoteHeight);
        }
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
        if (!isPolling) {
            logger.info("PollBlock to BlockListener，开始poll...，blockQueue's size is {}", blockQueue.size());
            onBlock();
        }
    }

    /**
     * BlockQueue pollBlock to BlockListener
     */
    private void onBlock() {
        isPolling = true;
        while (!blockQueue.isEmpty()) {
            syncListener.onBlock(blockQueue.poll());
            if (stopPoll) {
                break;
            }
        }
        isPolling = false;
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
        public SyncService build() {
            return new SyncService(this);
        }
    }
}
