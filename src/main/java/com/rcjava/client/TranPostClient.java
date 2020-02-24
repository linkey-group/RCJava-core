package com.rcjava.client;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rcjava.protos.Peer.Transaction;
import org.apache.commons.codec.binary.Hex;

/**
 * 构造交易，提交交易
 *
 * @author zyf
 */
public class TranPostClient extends RClient {

    private String host;

    public TranPostClient(String host) {
        this.host = host;
    }

    /**
     *
     * @param tran
     */
    public JSONObject postTranByString(Transaction tran) {
        String tranHex = Hex.encodeHexString(tran.toByteArray());
        String url = "http://" + host + "/transaction/postTranByString";
        return postJString(url, JSON.toJSONString(tranHex));
    }

    /**
     *
     * @param tran
     */
    public JSONObject postTranByStream(Transaction tran) {
        String url = "http://" + host + "/transaction/postTranStream";
        return postTranStream(url, tran);
    }


    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
