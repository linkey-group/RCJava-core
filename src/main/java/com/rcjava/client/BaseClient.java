package com.rcjava.client;

import com.alibaba.fastjson.JSONObject;

/**
 * @author zyf
 */
abstract class BaseClient {

    abstract JSONObject getJObject(String url);

    abstract JSONObject postJString(String url, String json);

    abstract JSONObject postBytes(String url, byte[] bytes);

}
