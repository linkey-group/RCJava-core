package com.rcjava.client;

import com.alibaba.fastjson.JSONObject;
import com.rcjava.protos.Peer.Transaction;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author zyf
 */
public class RClient {

    private CloseableHttpClient httpClient = HttpClientBuilder.create().build();

    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * @param url
     * @return
     */
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
     * @param url
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
     * @param json 要提交的json数据
     * @return 返回post结果
     */
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
     * @param url  请求路径
     * @param tran 交易字节流
     * @return 返回post的结果（该方法用于流式提交交易）
     */
    protected JSONObject postTranStream(String url, Transaction tran) {
        HttpPost post = new HttpPost(url);
        try {
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            // ******************创建临时文件，然后通过文件上传的方式******************
//            File tranFile = writeTranToFile(tran);
//            tranFile.deleteOnExit();
            // *******************************************************************
            HttpEntity reqEntity = builder
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                    .addBinaryBody("signedTrans", tran.toByteArray(), ContentType.APPLICATION_OCTET_STREAM, "tranByteArray")
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
        if (status >= 200 && status < 300) {
            HttpEntity resEntity = response.getEntity();
            String str = EntityUtils.toString(resEntity, "UTF-8");
            JSONObject result = JSONObject.parseObject(str);
            return result.getJSONObject("result") == null ? result : result.getJSONObject("result");
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
