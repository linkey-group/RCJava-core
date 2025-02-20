package com.rcjava.client;

import com.alibaba.fastjson2.JSONObject;
import com.rcjava.protos.Peer.Transaction;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.URL;
import java.util.UUID;

/**
 * @author zyf
 */
public class RClient implements BaseClient {

    private RequestConfig requestConfig;
    private PoolingHttpClientConnectionManager httpConnManager;
    private CloseableHttpClient httpClient;
    private CloseableHttpClient httpsClient;

    {
        requestConfig = RequestConfig.custom()
                .setConnectTimeout(20000)
                .setConnectionRequestTimeout(20000)
                .setSocketTimeout(20000)
                .build();
    }

    private Logger logger = LoggerFactory.getLogger(getClass());

    public RClient() {
        httpConnManager = new PoolingHttpClientConnectionManager();
        // 设置最大连接数
        httpConnManager.setMaxTotal(20);
        // 设置每个连接的路由数
        httpConnManager.setDefaultMaxPerRoute(5);
        httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(httpConnManager)
                .build();
    }

    public RClient(SSLContext sslContext) {
        httpConnManager = new PoolingHttpClientConnectionManager(
                RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("http", PlainConnectionSocketFactory.getSocketFactory())
                        .register("https", new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE))
                        .build()
        );
        // 设置最大连接数
        httpConnManager.setMaxTotal(20);
        // 设置每个连接的路由数
        httpConnManager.setDefaultMaxPerRoute(5);
        httpsClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(httpConnManager)
                .build();
    }


    /**
     * @param url pathParam
     * @return
     */
    @Override
    public JSONObject getJObject(String url) {
        try {
            URL reqUrl = new URL(url);
            HttpGet get = new HttpGet(url);
            if (reqUrl.getProtocol().equalsIgnoreCase(PROTOCOL_HTTP)) {
                return httpClient.execute(get, responseHandler);
            } else if (reqUrl.getProtocol().equalsIgnoreCase(PROTOCOL_HTTPS)) {
                return httpsClient.execute(get, responseHandler);
            } else {
                logger.error("暂不支持该协议: {}", reqUrl.getProtocol());
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * @param url
     * @return
     */
    @Override
    public InputStream getInputStream(String url) {
        try {
            URL reqUrl = new URL(url);
            HttpGet get = new HttpGet(url);
            if (reqUrl.getProtocol().equalsIgnoreCase(PROTOCOL_HTTP)) {
                HttpResponse response = httpClient.execute(get);
                return response.getEntity().getContent();
            } else if (reqUrl.getProtocol().equalsIgnoreCase(PROTOCOL_HTTPS)) {
                HttpResponse response = httpsClient.execute(get);
                return response.getEntity().getContent();
            } else {
                logger.error("暂不支持该协议: {}", reqUrl.getProtocol());
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * @param url pathParam
     * @return
     */
    protected HttpResponse getResponse(String url) {
        try {
            URL reqUrl = new URL(url);
            HttpGet get = new HttpGet(url);
            if (reqUrl.getProtocol().equalsIgnoreCase(PROTOCOL_HTTP)) {
                return httpClient.execute(get);
            } else if (reqUrl.getProtocol().equalsIgnoreCase(PROTOCOL_HTTPS)) {
                return httpsClient.execute(get);
            } else {
                logger.error("暂不支持该协议: {}", reqUrl.getProtocol());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }


    /**
     * post交易到RepChain
     *
     * @param url  请求路径
     * @param json 要提交的交易json数据
     * @return 返回post结果
     */
    @Override
    public JSONObject postJString(String url, String json) {
        try {
            URL reqUrl = new URL(url);
            HttpPost post = new HttpPost(url);
            //发送json数据需要设置contentType
            StringEntity reqEntity = new StringEntity(json, ContentType.APPLICATION_JSON);
            post.setEntity(reqEntity);
            if (reqUrl.getProtocol().equalsIgnoreCase(PROTOCOL_HTTP)) {
                return httpClient.execute(post, responseHandler);
            } else if (reqUrl.getProtocol().equalsIgnoreCase(PROTOCOL_HTTPS)) {
                return httpsClient.execute(post, responseHandler);
            } else {
                logger.error("暂不支持该协议: {}", reqUrl.getProtocol());
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * 提交交易
     *
     * @param url   请求路径
     * @param bytes 要post的交易字节数组
     * @return 返回post的结果（该方法用于流式提交交易）
     */
    @Override
    public JSONObject postBytes(String url, byte[] bytes) {
        String fileNameSuffix = UUID.randomUUID().toString();
        return this.postBytes(url, "signedTrans", bytes, "tranByteArray" + fileNameSuffix);
    }

    /**
     * 提交交易
     *
     * @param url      请求路径
     * @param name     参数名(fieldName/paramName)，服务端通过该参数获取相应的字节
     * @param bytes    要post的字节数组
     * @param fileName 文件名
     * @return 返回post的结果（该方法用于流式提交交易）
     */
    protected JSONObject postBytes(String url, String name, byte[] bytes, String fileName) {
        try {
            URL reqUrl = new URL(url);
            HttpPost post = new HttpPost(url);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            // ******************创建临时文件，然后通过文件上传的方式******************
//            File tranFile = writeTranToFile(tran);
//            tranFile.deleteOnExit();
            // *******************************************************************
            HttpEntity reqEntity = builder
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                    .addBinaryBody(name, bytes, ContentType.DEFAULT_BINARY, fileName)
                    .build();
            post.setEntity(reqEntity);
            if (reqUrl.getProtocol().equalsIgnoreCase(PROTOCOL_HTTP)) {
                return httpClient.execute(post, responseHandler);
            } else if (reqUrl.getProtocol().equalsIgnoreCase(PROTOCOL_HTTPS)) {
                return httpsClient.execute(post, responseHandler);
            } else {
                logger.error("暂不支持该协议: {}", reqUrl.getProtocol());
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * Create a custom response handler
     */
    ResponseHandler<JSONObject> responseHandler = response -> {
        int status = response.getStatusLine().getStatusCode();
        if (status >= HttpStatus.SC_OK && status < HttpStatus.SC_MULTIPLE_CHOICES) {
            HttpEntity resEntity = response.getEntity();
            String str = EntityUtils.toString(resEntity, "UTF-8");
            JSONObject result = JSONObject.parseObject(str);
            Object resObj = result.get("result");
            if (resObj instanceof JSONObject) {
                return (JSONObject) resObj;
            } else {
                return result;
            }
        } else {
            throw new HttpResponseException(status, "Unexpected response status: " + status);
        }
    };

    /**
     * @return
     */
    private File writeTranToFile(Transaction tran) {
        try {
            File tempFile = File.createTempFile("tran", null);
            OutputStream os = new FileOutputStream(tempFile);
            os.write(tran.toByteArray());
            os.close();
            return tempFile;
        } catch (IOException ioEx) {
            logger.error("将交易写入临时文件错误", ioEx.getMessage(), ioEx);
            ioEx.printStackTrace();
        }
        return null;
    }

}
