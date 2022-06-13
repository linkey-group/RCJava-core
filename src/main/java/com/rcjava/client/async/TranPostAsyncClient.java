package com.rcjava.client.async;


import com.alibaba.fastjson2.JSON;
import com.rcjava.protos.Peer.Transaction;
import org.apache.http.HttpResponse;

import javax.net.ssl.SSLContext;
import java.util.concurrent.Future;

/**
 * 构造交易，提交交易
 *
 * @author zyf
 */
public class TranPostAsyncClient extends RAsyncClient {

    private String host;

    private SSLContext sslContext;
    private boolean useSsl = false;

    private String PROTOCOL_HTTP = "http";
    private String PROTOCOL_HTTPS = "https";

    public TranPostAsyncClient(String host) {
        this.host = host;
    }

    public TranPostAsyncClient(String host, SSLContext sslContext) {
        super(sslContext);
        this.host = host;
        this.sslContext = sslContext;
        this.useSsl = true;
    }

    /**
     * 提交签名交易，以十六进制字符串
     *
     * @param tranHexString 签名交易的十六进制字符串
     */
    public Future<HttpResponse> postSignedTran(String tranHexString) {
        String protocol = useSsl ? PROTOCOL_HTTPS : PROTOCOL_HTTP;
        String url = String.format("%s://%s/transaction/postTranByString", protocol, host);
        return postJString(url, JSON.toJSONString(tranHexString));
    }

    /**
     * 提交签名交易，以字节方式
     *
     * @param tran 签名交易
     */
    public Future<HttpResponse> postSignedTran(Transaction tran) {
        String protocol = useSsl ? PROTOCOL_HTTPS : PROTOCOL_HTTP;
        String url = String.format("%s://%s/transaction/postTranStream", protocol, host);
        return postBytes(url, "signedTrans", tran.toByteArray(), "tranByteArray");
    }


    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
