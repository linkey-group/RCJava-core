package com.rcjava.client;

import com.alibaba.fastjson2.JSONObject;

import java.io.InputStream;

/**
 * @author zyf
 */
abstract class BaseClient {

    /**
     * getJsonObject
     *
     * @param url
     * @return
     */
    abstract JSONObject getJObject(String url);

    /**
     * 获取输入流
     *
     * @param url
     * @return
     */
    abstract InputStream getInputStream(String url);

    /**
     * 提交json字符串
     *
     * @param url
     * @param json
     * @return
     */
    abstract JSONObject postJString(String url, String json);

    /**
     * post字节
     *
     * @param url
     * @param bytes
     * @return
     */
    abstract JSONObject postBytes(String url, byte[] bytes);

}
