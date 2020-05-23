package com.rcjava.client;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rcjava.protos.Peer.Transaction;

/**
 * 构造交易，提交交易
 *
 * @author zyf
 */
public class TranPostClient {

    private String host;

    private boolean useJavaImpl = false;

    private RClient rClient = new RClient();
    private RCJavaClient rcJavaClient = new RCJavaClient();

    public TranPostClient(String host) {
        this.host = host;
    }

    /**
     * 提交签名交易，以十六进制字符串
     *
     * @param tranHexString 签名交易的十六进制字符串
     */
    public JSONObject postSignedTran(String tranHexString) {
        String url = "http://" + host + "/transaction/postTranByString";
        BaseClient client = useJavaImpl ? rcJavaClient : rClient;
        return client.postJString(url, JSON.toJSONString(tranHexString));
    }

    /**
     * 提交签名交易，以字节方式
     *
     * @param tran 签名交易
     */
    public JSONObject postSignedTran(Transaction tran) {
        String url = "http://" + host + "/transaction/postTranStream";
        BaseClient client = useJavaImpl ? rcJavaClient : rClient;
        return client.postBytes(url, tran.toByteArray());
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
    }
}
