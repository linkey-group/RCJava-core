package com.rcjava.client;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author zyf
 */
public class RCJavaClient extends BaseClient {

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

    /**
     * HttpURLConnection 初始化
     *
     * @param httpUrl request url
     * @return HttpURLConnection 对象
     */
    public static HttpURLConnection getHttpConnection(String httpUrl) {

        try {
            URI uri = new URI(httpUrl);
            URL url = uri.toURL();
            URLConnection urlConnection = url.openConnection();
            return (HttpURLConnection) urlConnection;
        } catch (URISyntaxException | IOException ex) {
            logger.error("初始化连接出错！", ex);
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
    JSONObject getJObject(String url) {

        HttpURLConnection urlConnection = getHttpConnection(url);
        InputStream inputStream = null;

        try {

            urlConnection.setRequestMethod("GET");
            urlConnection.setConnectTimeout(5000);

            urlConnection.connect();

            inputStream = urlConnection.getInputStream();

            int code = urlConnection.getResponseCode();
            if (code >= HttpURLConnection.HTTP_OK && code < HttpURLConnection.HTTP_MULT_CHOICE) {
                return convertStreamToJObject(inputStream);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *
     * @param url
     * @return
     */
    @Override
    InputStream getInputStream(String url) {

        HttpURLConnection urlConnection = getHttpConnection(url);
        InputStream inputStream = null;

        try {

            urlConnection.setRequestMethod("GET");
            urlConnection.setConnectTimeout(5000);

            urlConnection.connect();

            inputStream = urlConnection.getInputStream();

            int code = urlConnection.getResponseCode();
            if (code >= HttpURLConnection.HTTP_OK && code < HttpURLConnection.HTTP_MULT_CHOICE) {
                return inputStream;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return inputStream;
    }

    /**
     * post--jsonString
     *
     * @param url
     * @param json
     * @return
     */
    @Override
    JSONObject postJString(String url, String json) {

        HttpURLConnection urlConnection = getHttpConnection(url);

        try {

            urlConnection.setDoOutput(true);
            urlConnection.setUseCaches(false);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-type", "application/json");
            urlConnection.setRequestProperty("Connection", "Keep-Alive");
            urlConnection.setConnectTimeout(5000);

            urlConnection.connect();

            OutputStream outputStream = urlConnection.getOutputStream();
            outputStream.write(json.getBytes(Charset.forName("UTF-8")));
            outputStream.close();
//            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
//            objectOutputStream.writeObject(json);
//            objectOutputStream.flush();
//            objectOutputStream.close();

            InputStream inputStream = urlConnection.getInputStream();

            int code = urlConnection.getResponseCode();
            if (code >= HttpURLConnection.HTTP_OK && code < HttpURLConnection.HTTP_MULT_CHOICE) {
                return convertStreamToJObject(inputStream);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *
     * @param url
     * @param bytes
     * @return
     */
    @Override
    JSONObject postBytes(String url, byte[] bytes) {
        String fileNameSuffix = UUID.randomUUID().toString();
        return this.postBytes(url, "signedTrans", bytes, "tranByteArray" + fileNameSuffix);
    }

    /**
     *
     * @param url
     * @param name
     * @param bytes
     * @param fileName
     * @return
     */
    JSONObject postBytes(String url, String name, byte[] bytes, String fileName) {

        HttpURLConnection urlConnection = getHttpConnection(url);

        try {

            urlConnection.setDoOutput(true);
            urlConnection.setUseCaches(false);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Connection", "Keep-Alive");
            urlConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
            urlConnection.setRequestProperty("Content-Transfer-Encoding", "binary");
            urlConnection.setConnectTimeout(5000);

            urlConnection.connect();

            OutputStream outputStream = urlConnection.getOutputStream();

            String field = String.format(fieldStr, name, fileName, "application/octet-stream");

            outputStream.write(field.getBytes(Charset.forName("UTF-8")));
            outputStream.write(bytes);
            outputStream.write(endStr.getBytes(Charset.forName("UTF-8")));

            outputStream.flush();
            outputStream.close();

            InputStream inputStream = urlConnection.getInputStream();

            int code = urlConnection.getResponseCode();
            if (code >= HttpURLConnection.HTTP_OK && code < HttpURLConnection.HTTP_MULT_CHOICE) {
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
        Object resObj = result.get("result");
        if (resObj instanceof JSONObject) {
            return (JSONObject) resObj;
        } else {
            return result;
        }
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
