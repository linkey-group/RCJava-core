package com.rcjava.client.async;


import com.alibaba.fastjson2.JSON;
import com.rcjava.protos.Peer.Transaction;
import org.apache.http.HttpResponse;

import java.util.concurrent.Future;

/**
 * 构造交易，提交交易
 *
 * @author zyf
 */
public class TranPostAsyncClient extends RAsyncClient {

    private String host;

    public TranPostAsyncClient(String host) {
        this.host = host;
    }

    /**
     * 提交签名交易，以十六进制字符串
     *
     * @param tranHexString 签名交易的十六进制字符串
     */
    public Future<HttpResponse> postSignedTran(String tranHexString) {
        String url = "http://" + host + "/transaction/postTranByString";
        return postJString(url, JSON.toJSONString(tranHexString));
    }

    /**
     * 提交签名交易，以字节方式
     *
     * @param tran 签名交易
     */
    public Future<HttpResponse> postSignedTran(Transaction tran) {
        String url = "http://" + host + "/transaction/postTranStream";
        return postBytes(url, "signedTrans", tran.toByteArray(), "tranByteArray");
    }


    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
