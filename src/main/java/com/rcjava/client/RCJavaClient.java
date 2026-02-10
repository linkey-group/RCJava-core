package com.rcjava.client;

import com.alibaba.fastjson2.JSONObject;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author zyf
 */
public class RCJavaClient implements BaseClient {

    private static Logger logger = LoggerFactory.getLogger(RCJavaClient.class);

    // 换行符
    private static String lineSeparator = System.lineSeparator();

    // 定义数据分隔线
    private static String boundaryPrefix = "--";

    private static String BOUNDARY = UUID.randomUUID().toString().replace("-", "");

    private static String fieldStr = new StringBuilder()
            .append(boundaryPrefix).append(BOUNDARY).append(lineSeparator)
            .append("Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"").append(lineSeparator)
            .append("Content-Type:%s").append(lineSeparator).append(lineSeparator)
            .toString();

    // 定义最后数据分隔线，即"--" + BOUNDARY + "--"
    private static String endStr = new StringBuilder()
            .append(lineSeparator)
            .append(boundaryPrefix).append(BOUNDARY).append(boundaryPrefix)
            .append(lineSeparator)
            .toString();

    private SSLContext sslContext;

    public RCJavaClient() {
    }

    public RCJavaClient(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    /**
     * URLConnection 初始化
     *
     * @param url request url
     * @return URLConnection 对象
     */
    public static URLConnection getUrlConnection(URL url) {

        try {
            return url.openConnection();
        } catch (IOException ex) {
            logger.error("初始化连接出错: {}", ex.getMessage(), ex);
        }

        return null;
    }

    /**
     * getResource
     *
     * @param url
     * @return
     */
    @Override
    public JSONObject getJObject(String url) {

        try {
            URL reqUrl = new URL(url);
            String protocol = reqUrl.getProtocol();

            URLConnection urlConnection = reqUrl.openConnection();
            urlConnection.setConnectTimeout(20000);
            urlConnection.setReadTimeout(20000);

            int code = -1;

            if (protocol.equalsIgnoreCase(PROTOCOL_HTTP)) {
                HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.connect();
                code = httpURLConnection.getResponseCode();
            } else if (protocol.equalsIgnoreCase(PROTOCOL_HTTPS)) {
                HttpsURLConnection httpsURLConnection = (HttpsURLConnection) urlConnection;
                httpsURLConnection.setRequestMethod("GET");
                httpsURLConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                httpsURLConnection.setHostnameVerifier(NoopHostnameVerifier.INSTANCE);
                httpsURLConnection.connect();
                code = httpsURLConnection.getResponseCode();
            } else {
                logger.error("暂不支持该协议: {}", reqUrl.getProtocol());
            }

            if (code >= HttpURLConnection.HTTP_OK && code < HttpURLConnection.HTTP_MULT_CHOICE) {
                InputStream inputStream = urlConnection.getInputStream();
                return convertStreamToJObject(inputStream);
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * Updated by 北京连琪科技有限公司.
     * @param url
     * @return
     */
    @Override
    public InputStream getInputStream(String url) {

        HttpURLConnection httpURLConnection = null;
        HttpsURLConnection httpsURLConnection = null;

        try {

            URL reqUrl = new URL(url);
            String protocol = reqUrl.getProtocol();

            URLConnection urlConnection = reqUrl.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);

            int code = -1;

            if (protocol.equalsIgnoreCase(PROTOCOL_HTTP)) {
                httpURLConnection = (HttpURLConnection) urlConnection;
                httpURLConnection.setRequestMethod("GET");
                urlConnection.connect();
                code = httpURLConnection.getResponseCode();
            } else if (protocol.equalsIgnoreCase(PROTOCOL_HTTPS)) {
                httpsURLConnection = (HttpsURLConnection) urlConnection;
                httpsURLConnection.setRequestMethod("GET");
                httpsURLConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                httpsURLConnection.setHostnameVerifier(NoopHostnameVerifier.INSTANCE);
                urlConnection.connect();
                code = httpsURLConnection.getResponseCode();
            } else {
                logger.error("暂不支持该协议: {}", reqUrl.getProtocol());
            }

            if (code >= HttpURLConnection.HTTP_OK && code < HttpURLConnection.HTTP_MULT_CHOICE) {
                InputStream inputStream = urlConnection.getInputStream();
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();

                int nRead;
                byte[] data = new byte[16384];

                while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }

                buffer.flush();
                inputStream.close();

                // 断开连接
                if (httpURLConnection != null) httpURLConnection.disconnect();
                if (httpsURLConnection != null) httpsURLConnection.disconnect();

                // 返回内存中的数据流
                return new ByteArrayInputStream(buffer.toByteArray());
            } else {
                // 非成功响应码，关闭连接
                if (httpURLConnection != null) httpURLConnection.disconnect();
                if (httpsURLConnection != null) httpsURLConnection.disconnect();
            }
        } catch (IOException e) {
            logger.error("获取输入流失败", e);
            // 发生异常时关闭连接
            if (httpURLConnection != null) httpURLConnection.disconnect();
            if (httpsURLConnection != null) httpsURLConnection.disconnect();
        }
        return null;
    }

    /**
     * post--jsonString
     *
     * @param url
     * @param json
     * @return
     */
    @Override
    public JSONObject postJString(String url, String json) {

        try {

            URL reqUrl = new URL(url);
            String protocol = reqUrl.getProtocol();

            URLConnection urlConnection = reqUrl.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setUseCaches(false);
            urlConnection.setRequestProperty("Content-type", "application/json");
            urlConnection.setRequestProperty("Connection", "Keep-Alive");
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);

            int code = -1;

            if (protocol.equalsIgnoreCase(PROTOCOL_HTTP)) {
                HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.connect();

                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(json.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
                outputStream.close();
//            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
//            objectOutputStream.writeObject(json);
//            objectOutputStream.flush();
//            objectOutputStream.close();

                code = httpURLConnection.getResponseCode();
            } else if (protocol.equalsIgnoreCase(PROTOCOL_HTTPS)) {
                HttpsURLConnection httpsURLConnection = (HttpsURLConnection) urlConnection;
                httpsURLConnection.setRequestMethod("POST");
                httpsURLConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                httpsURLConnection.setHostnameVerifier(NoopHostnameVerifier.INSTANCE);
                httpsURLConnection.connect();

                OutputStream outputStream = httpsURLConnection.getOutputStream();
                outputStream.write(json.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
                outputStream.close();

                code = httpsURLConnection.getResponseCode();
            } else {
                logger.error("暂不支持该协议: {}", reqUrl.getProtocol());
            }

            if (code >= HttpURLConnection.HTTP_OK && code < HttpURLConnection.HTTP_MULT_CHOICE) {
                InputStream inputStream = urlConnection.getInputStream();
                return convertStreamToJObject(inputStream);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param url
     * @param bytes
     * @return
     */
    @Override
    public JSONObject postBytes(String url, byte[] bytes) {
        String fileNameSuffix = UUID.randomUUID().toString();
        return this.postBytes(url, "signedTrans", bytes, "tranByteArray" + fileNameSuffix);
    }

    /**
     * @param url
     * @param name
     * @param bytes
     * @param fileName
     * @return
     */
    JSONObject postBytes(String url, String name, byte[] bytes, String fileName) {

        try {

            URL reqUrl = new URL(url);
            String protocol = reqUrl.getProtocol();

            URLConnection urlConnection = reqUrl.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setUseCaches(false);
            urlConnection.setRequestProperty("Connection", "Keep-Alive");
            urlConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
            urlConnection.setRequestProperty("Content-Transfer-Encoding", "binary");
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);

            int code = -1;

            if (protocol.equalsIgnoreCase(PROTOCOL_HTTP)) {
                HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.connect();

                OutputStream outputStream = urlConnection.getOutputStream();
                String field = String.format(fieldStr, name, fileName, "application/octet-stream");
                outputStream.write(field.getBytes(StandardCharsets.UTF_8));
                outputStream.write(bytes);
                outputStream.write(endStr.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
                outputStream.close();

                code = httpURLConnection.getResponseCode();
            } else if (protocol.equalsIgnoreCase(PROTOCOL_HTTPS)) {
                HttpsURLConnection httpsURLConnection = (HttpsURLConnection) urlConnection;
                httpsURLConnection.setRequestMethod("POST");
                httpsURLConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                httpsURLConnection.setHostnameVerifier(NoopHostnameVerifier.INSTANCE);
                httpsURLConnection.connect();

                OutputStream outputStream = urlConnection.getOutputStream();
                String field = String.format(fieldStr, name, fileName, "application/octet-stream");
                outputStream.write(field.getBytes(StandardCharsets.UTF_8));
                outputStream.write(bytes);
                outputStream.write(endStr.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
                outputStream.close();

                code = httpsURLConnection.getResponseCode();
            } else {
                logger.error("暂不支持该协议: {}", reqUrl.getProtocol());
            }

            if (code >= HttpURLConnection.HTTP_OK && code < HttpURLConnection.HTTP_MULT_CHOICE) {
                InputStream inputStream = urlConnection.getInputStream();
                return convertStreamToJObject(inputStream);
            } else {
                logger.info("response code is {}", code);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 将InputStream转为JSONObject
     *
     * @param inputStream
     * @return
     */
    private JSONObject convertStreamToJObject(InputStream inputStream) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String str = br.lines().collect(Collectors.joining(System.lineSeparator()));
        inputStream.close();
        JSONObject result = JSONObject.parseObject(str);
        return result;
    }

    /**
     * 将InputStream转为String
     *
     * @param inputStream
     * @param charset
     * @return
     * @throws IOException
     */
    private String convertStreamToString(InputStream inputStream, Charset charset) throws IOException {

        StringBuilder stringBuilder = new StringBuilder();
        String line = null;

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }

        return stringBuilder.toString();
    }
}
