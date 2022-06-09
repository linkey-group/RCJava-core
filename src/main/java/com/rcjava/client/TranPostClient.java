package com.rcjava.client;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.rcjava.client.callback.TranCallBack;
import com.rcjava.client.callback.TranCallBackInfo;
import com.rcjava.client.callback.TranCallBackMgr;
import com.rcjava.protos.Peer.Transaction;

import javax.net.ssl.SSLContext;

/**
 * 构造交易，提交交易
 *
 * @author zyf
 */
public class TranPostClient {

    private String host;
    private boolean useSsl = false;
    private boolean useJavaImpl = false;

    private SSLContext sslContext;

    private String PROTOCOL_HTTP = "http";
    private String PROTOCOL_HTTPS = "https";

    private RClient rClient = new RClient();
    private RCJavaClient rcJavaClient = new RCJavaClient();

    private BaseClient client = rClient;

    public TranPostClient(String host) {
        this.host = host;
    }

    public TranPostClient(String host, SSLContext sslContext) {
        this.host = host;
        this.sslContext = sslContext;
        this.rClient = new RClient(sslContext);
        this.rcJavaClient = new RCJavaClient(sslContext);
        this.client = rClient;
        this.useSsl = true;
    }

    /**
     * 提交签名交易，以十六进制字符串
     *
     * @param tranHexString 签名交易的十六进制字符串
     */
    public JSONObject postSignedTran(String tranHexString) {
        String protocol = useSsl ? PROTOCOL_HTTPS : PROTOCOL_HTTP;
        String url = String.format("%s://%s/transaction/postTranByString", protocol, host);
        return client.postJString(url, JSON.toJSONString(tranHexString));
    }

    /**
     * 提交签名交易，以字节方式
     *
     * @param tran 签名交易
     */
    public JSONObject postSignedTran(Transaction tran) {
        String protocol = useSsl ? PROTOCOL_HTTPS : PROTOCOL_HTTP;
        String url = String.format("%s://%s/transaction/postTranStream", protocol, host);
        return client.postBytes(url, tran.toByteArray());
    }

    /**
     * 异步提交，如果监测到交易入块，则回调成功，如果超时，则回调失败，只能处理http
     *
     * @param tran         交易
     * @param tranCallBack 回调接口
     * @param timeout      回调超时 -> 分钟
     * @return
     */
    @Deprecated
    public void postSignedTran(Transaction tran, TranCallBack tranCallBack, long timeout) {
        String txid = tran.getId();
        TranCallBackMgr tranCallBackMgr = TranCallBackMgr.getInstance(host);
        tranCallBackMgr.addTranCallBackInfo(new TranCallBackInfo(txid, tranCallBack, timeout));
        JSONObject jsonObject = postSignedTran(tran);
        if (jsonObject.containsKey("err")) {
            // 预执行失败
            tranCallBack.preTransactionResult(tran.getId(), false, jsonObject);
            tranCallBack.isOnChain(txid, false);
            tranCallBackMgr.removeTranCallBackInfo(txid);
        } else {
            // 预执行成功
            tranCallBack.preTransactionResult(tran.getId(), true, jsonObject);
        }
    }


    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public boolean isUseJavaImpl() {
        return useJavaImpl;
    }

    public void setUseJavaImpl(boolean useJavaImpl) {
        this.useJavaImpl = useJavaImpl;
        this.client = useJavaImpl ? rcJavaClient : rClient;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }
}
