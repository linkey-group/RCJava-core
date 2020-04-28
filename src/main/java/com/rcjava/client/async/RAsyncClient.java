package com.rcjava.client.async;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.rcjava.protos.Peer;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author zyf
 */
public class RAsyncClient {

    // 设置连接池大小
    private static ConnectingIOReactor ioReactor;
    // 异步httpClient
    private static CloseableHttpAsyncClient httpAsyncClient;

    static {
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(5000).setConnectionRequestTimeout(10000).setSocketTimeout(5000).build();
        //配置io线程
        IOReactorConfig ioReactorConfig = IOReactorConfig.custom().setIoThreadCount(Runtime.getRuntime().availableProcessors()).setSoKeepAlive(true).build();
        try {
            ioReactor = new DefaultConnectingIOReactor(ioReactorConfig);
        } catch (IOReactorException e) {
            e.printStackTrace();
        }
        PoolingNHttpClientConnectionManager connManager = new PoolingNHttpClientConnectionManager(ioReactor);
        // 设置最大连接数
        connManager.setMaxTotal(200);
        // 设置每个连接的路由数
        connManager.setDefaultMaxPerRoute(20);

        httpAsyncClient = HttpAsyncClients.custom().setDefaultRequestConfig(requestConfig).setConnectionManager(connManager).build();
        httpAsyncClient.start();

    }

    private Logger logger = LoggerFactory.getLogger(getClass());
    private static Logger staticLogger = LoggerFactory.getLogger(RAsyncClient.class);


    /**
     * 异步get请求，同步get请求请看{@link com.rcjava.client.RClient#getResponse(String)}
     *
     * @param url pathParam
     * @return
     */
    protected Future<HttpResponse> getResponse(String url) {
        HttpGet get = new HttpGet(url);
        try {
            return httpAsyncClient.execute(get, new AsyncResponse(url, null));
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return null;
    }


    /**
     * 异步post交易{@code JsonString}到RepChain；同步post交易请看{@link com.rcjava.client.RClient#postJString(String, String)}.
     *
     * @param url  请求路径
     * @param json 要提交的json数据
     * @return 返回post结果
     */
    protected Future<HttpResponse> postJString(String url, String json) {
        HttpPost post = new HttpPost(url);
        try {
            //发送json数据需要设置contentType
            NStringEntity reqEntity = new NStringEntity(json, ContentType.APPLICATION_JSON);
            post.setEntity(reqEntity);
            return httpAsyncClient.execute(post, new AsyncResponse(url, json));
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * 异步post交易{@code byte[]}到RepChain；同步post交易请看{@link com.rcjava.client.RClient#postBytes(String, String, byte[], String)}.
     *
     * @param url      请求路径
     * @param name     参数名(fieldName/paramName)，服务端通过该参数获取相应的字节
     * @param bytes    要post的字节数组
     * @param fileName 文件名
     * @return 返回post的结果（该方法用于流式提交交易）
     */
    protected Future<HttpResponse> postBytes(String url, String name, byte[] bytes, String fileName) {
        HttpPost post = new HttpPost(url);
        try {
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            HttpEntity reqEntity = builder
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                    .addBinaryBody(name, bytes, ContentType.DEFAULT_BINARY, fileName)
                    .build();
            post.setEntity(reqEntity);
            return httpAsyncClient.execute(post, new AsyncResponse(url, bytes));
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * 请求的回调函数
     *
     * @param <T>
     */
    static class AsyncResponse<T> implements FutureCallback<HttpResponse> {

        private Logger logger = LoggerFactory.getLogger(getClass());

        private String url;
        private T data;

        public AsyncResponse(String url, T data) {
            this.url = url;
            this.data = data;
        }

        @Override
        public void completed(HttpResponse httpResponse) {
            if (data instanceof String) {
                logger.info("request's url is {}, jsonString is {}", url, data);
                String hexString = (String) JSON.parse((String) data);
                byte[] bytes = Hex.decode(hexString);
                try {
                    Peer.Transaction tran = Peer.Transaction.parseFrom(bytes);
                    logger.info("request's url is {}, Transaction id is {}", url, tran.getId());
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
            } else if (data instanceof byte[]) {
                logger.info("request's url is {}, byte[] is {}", url, Arrays.toString((byte[]) data));
                try {
                    byte[] bytes = (byte[]) data;
                    Peer.Transaction tran = Peer.Transaction.parseFrom(bytes);
                    logger.info("request's url is {}, Transaction id is {}", url, tran.getId());
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
            } else {
                logger.info("request's url is {}, data is {}", url, data);
            }
        }

        @Override
        public void failed(Exception e) {
            logger.error("request's url is {}, errorMsg is {}", url, e.getMessage(), e);
        }

        @Override
        public void cancelled() {
            logger.info("request's url is {}, request cancelled", url);
        }
    }

    /**
     * 从future中解析结果
     *
     * @param responseFuture
     * @return
     */
    protected static JSONObject resolveHttpResponseFuture(Future<HttpResponse> responseFuture) {
        HttpResponse httpResponse = null;
        try {
            httpResponse = responseFuture.get(20, TimeUnit.SECONDS);
            int status = httpResponse.getStatusLine().getStatusCode();
            if (status >= HttpStatus.SC_OK && status < HttpStatus.SC_MULTIPLE_CHOICES) {
                HttpEntity resEntity = httpResponse.getEntity();
                String str = EntityUtils.toString(resEntity, "UTF-8");
                EntityUtils.consumeQuietly(resEntity);
                JSONObject result = JSONObject.parseObject(str);
                return result.getJSONObject("result") == null ? result : result.getJSONObject("result");
            } else {
                staticLogger.error("Unexpected response status: {}", status, new HttpResponseException(status, "Unexpected response status: " + status));
            }
        } catch (InterruptedException | ExecutionException | TimeoutException | IOException e) {
            staticLogger.error("errMsg is {}", e.getMessage(), e);
        }
        return null;
    }
}
