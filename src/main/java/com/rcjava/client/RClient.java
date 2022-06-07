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
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.UUID;

/**
 * @author zyf
 */
public class RClient extends BaseClient {

    private static RequestConfig requestConfig;

    private static PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();

    static {
        requestConfig = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(5000)
                .setSocketTimeout(5000)
                .build();
        // 设置最大连接数
        connManager.setMaxTotal(200);
        // 设置每个连接的路由数
        connManager.setDefaultMaxPerRoute(20);
    }

    private static CloseableHttpClient httpClient = HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(connManager)
            .build();

    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * @param url pathParam
     * @return
     */
    @Override
    protected JSONObject getJObject(String url) {
        HttpGet get = new HttpGet(url);
        try {
            return httpClient.execute(get, responseHandler);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return null;
    }

    /**
     *
     * @param url
     * @return
     */
    @Override
    protected InputStream getInputStream(String url) {
        HttpGet get = new HttpGet(url);
        InputStream inputStream = null;
        try {
            HttpResponse response = httpClient.execute(get);
            inputStream = response.getEntity().getContent();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return inputStream;
    }

    /**
     * @param url pathParam
     * @return
     */
    protected HttpResponse getResponse(String url) {
        HttpGet get = new HttpGet(url);
        HttpResponse response = null;
        try {
            response = httpClient.execute(get);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return response;
    }


    /**
     * post交易到RepChain
     *
     * @param url  请求路径
     * @param json 要提交的交易json数据
     * @return 返回post结果
     */
    @Override
    protected JSONObject postJString(String url, String json) {
        HttpPost post = new HttpPost(url);
        try {
            //发送json数据需要设置contentType
            StringEntity reqEntity = new StringEntity(json, ContentType.APPLICATION_JSON);
            post.setEntity(reqEntity);
            return httpClient.execute(post, responseHandler);
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
    protected JSONObject postBytes(String url, byte[] bytes) {
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
        HttpPost post = new HttpPost(url);
        try {
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
            return httpClient.execute(post, responseHandler);
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
