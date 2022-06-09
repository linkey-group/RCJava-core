package com.rcjava.client.callback;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketState;
import com.rcjava.client.ChainInfoClient;
import com.rcjava.client.RSubClient;
import com.rcjava.protos.Peer;
import com.rcjava.ws.BlockObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 用来处理回调信息，只能处理http
 *
 * @author jby
 * @author zyf
 */
@Deprecated
public class TranCallBackMgr implements BlockObserver {

    private Logger logger = LoggerFactory.getLogger(getClass());
    private static Logger logger_static = LoggerFactory.getLogger(TranCallBackMgr.class);

    private String host;
    private static ChainInfoClient chainInfoClient;
    private static RSubClient rSubClient;
    // 单例模式
    private static TranCallBackMgr instance = null;

    // 10s间隔
    private static long monitorDelay = 10L;

    private ConcurrentHashMap<String, TranCallBackInfo> tranCallBackTasks;


    private static ThreadFactory callBackPoolFactory = new ThreadFactoryBuilder().setNameFormat("callBack-pool-%d").build();
    private static ThreadPoolExecutor callBackThreadPool = new ThreadPoolExecutor(20, 40, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(1024), callBackPoolFactory, new ThreadPoolExecutor.AbortPolicy());

    private ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    private TranCallBackMgr(String host) {
        this.host = host;
        this.tranCallBackTasks = new ConcurrentHashMap<>();
    }

    /**
     * 单例模式构建Mgr
     *
     * @param host
     * @return
     */
    public static TranCallBackMgr getInstance(String host) {
        if (instance == null) {
            synchronized (TranCallBackMgr.class) {
                if (instance == null) {
                    instance = new TranCallBackMgr(host);
                    try {
                        chainInfoClient = new ChainInfoClient(host);
                        rSubClient = RSubClient.getRSubClient(host);
                        if (rSubClient.getSocket() == null || (rSubClient.getSocket() != null && !rSubClient.isopen())) {
                            rSubClient.connect();
                        }
                        rSubClient.getBlkListener().registerBlkObserver(instance);
                        instance.startMonitor();
                    } catch (WebSocketException | IOException e) {
                        logger_static.error("getInstance error, errorMsg is {}", e.getMessage(), e);
                    }
                }
            }
        }
        return instance;
    }

    /**
     * 开启回调监控
     */
    private void startMonitor() {
        scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!tranCallBackTasks.isEmpty()) {
                    List<String> taskCollector = new ArrayList<>();
                    for (String key : tranCallBackTasks.keySet()) {
                        TranCallBackInfo tranCallBackInfo = tranCallBackTasks.get(key);
                        if (tranCallBackInfo == null) {
                            taskCollector.add(key);
                        } else if (tranCallBackInfo.isOnChain()) {
                            // 已经上链成功
                            startCallBack(tranCallBackInfo);
                            taskCollector.add(key);
                        } else if (tranCallBackInfo.isCallBackTimeOut()) {
                            // 订阅超时了
                            startCallBack(tranCallBackInfo);
                            taskCollector.add(key);
                        } else if (tranCallBackInfo.isPullTimeOut()) {
                            // 拉取过了间隔了
                            tranCallBackInfo.setLastPullTime(System.currentTimeMillis());
                            startPull(tranCallBackInfo);
                        }
                    }
                    // 确认过的txid，可能是出块了，可能是订阅超时的，都移除掉
                    taskCollector.forEach(txid -> tranCallBackTasks.remove(txid));
                    // 如果断了，重连
                    checkWebSocket();
                }
            } catch (Exception ex) {
                logger.error("callBackMonitor error, errorMsg is {}", ex.getMessage(), ex);
                ex.printStackTrace();
            }
        }, 10, monitorDelay, TimeUnit.SECONDS);
    }

    /**
     * 1、回调超时，通知失败；2、订阅到了，回调成功
     *
     * @param tranCallBackInfo
     */
    private void startCallBack(TranCallBackInfo tranCallBackInfo) {
        if (tranCallBackInfo != null) {
            callBackThreadPool.execute(tranCallBackInfo::onChain);
            tranCallBackTasks.remove(tranCallBackInfo.getTxid());
        }
    }

    /**
     * 没有订阅到，试着从链上检索，如果订阅到了，通知成功，否则不做任何操作
     *
     * @param tranCallBackInfo
     */
    private void startPull(TranCallBackInfo tranCallBackInfo) {
        if (tranCallBackInfo != null) {
            callBackThreadPool.execute(() -> {
                Peer.Transaction transaction = chainInfoClient.getTranByTranId(tranCallBackInfo.getTxid());
                if (transaction != null) {
                    tranCallBackInfo.setOnChain(true);
                    tranCallBackInfo.onChain();
                    logger.info("通过pull方式查询到交易，交易{}已上链", transaction.getId());
                }
            });
        }
    }

    /**
     * @param txid
     * @param tranCallBack
     * @param callBackTimeOut
     */
    public void addTranCallBackInfo(String txid, TranCallBack tranCallBack, long callBackTimeOut) {
        tranCallBackTasks.put(txid, new TranCallBackInfo(txid, tranCallBack, callBackTimeOut));
    }

    /**
     * @param tranCallBackInfo
     */
    public void addTranCallBackInfo(TranCallBackInfo tranCallBackInfo) {
        tranCallBackTasks.put(tranCallBackInfo.getTxid(), tranCallBackInfo);
    }

    /**
     * @param txid
     */
    public void removeTranCallBackInfo(String txid) {
        tranCallBackTasks.remove(txid);
    }

    /**
     * 检查webSocket，如果断掉则重连
     */
    private void checkWebSocket() {
        WebSocket ws = rSubClient.getSocket();
        if (!ws.isOpen() || ws.getSocket().isClosed() || ws.getState() == WebSocketState.CLOSED) {
            try {
                rSubClient.reconnect();
            } catch (IOException | WebSocketException e) {
                logger.error("checkWebSocket error, errorMsg is {}", e.getMessage(), e);
            }
        }
    }

    /**
     * @param block 回调的块数据
     */
    @Override
    public void onMessage(Peer.Block block) {
        for (Peer.Transaction transaction : block.getTransactionsList()) {
            if (tranCallBackTasks.containsKey(transaction.getId())) {
                TranCallBackInfo tranCallBackInfo = tranCallBackTasks.get(transaction.getId());
                tranCallBackInfo.setOnChain(true);
                startCallBack(tranCallBackInfo);
                logger.info("通过订阅方式订阅到交易，交易{}已上链", transaction.getId());
            }
        }
    }
}
