package com.rcjava.client.callback;

import com.alibaba.fastjson.JSONObject;


/**
 * 异步提交交易的回调
 *
 * @author zyf
 */
public interface TranCallBack {

    /**
     * 预执行结果
     *
     * @param txid
     * @param flag
     * @param result
     */
    void preTransactionResult(String txid, boolean flag, JSONObject result);

    /**
     * 是否上链成功
     *
     * @param txid
     * @param result
     */
    void isOnChain(String txid, boolean result);
}
