package com.rcjava.client;

import com.alibaba.fastjson2.JSONObject;

import java.io.InputStream;

/**
 * @author zyf
 */
interface BaseClient {

    String PROTOCOL_HTTP = "http";
    String PROTOCOL_HTTPS = "https";

    /**
     * getJsonObject
     *
     * @param url
     * @return
     */
    JSONObject getJObject(String url);

    /**
     * 获取输入流
     *
     * @param url
     * @return
     */
    InputStream getInputStream(String url);

    /**
     * 提交json字符串
     *
     * @param url
     * @param json
     * @return
     */
    JSONObject postJString(String url, String json);

    /**
     * post字节
     *
     * @param url
     * @param bytes
     * @return
     */
    JSONObject postBytes(String url, byte[] bytes);

}
