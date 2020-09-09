package com.rcjava.client.callback;

/**
 * 用来保存TranCallBack的一些信息
 *
 * @author jby
 * @author zyf
 */
public class TranCallBackInfo {

    private String txid;
    private TranCallBack tranCallBack;

    // minutes
    private long callBackTimeOut = 15;
    // seconds
    private long pullInterval = 20;

    private long callBackStart = 0L;
    private long lastPullTime = 0L;

    private boolean onChain = false;

    /**
     * @param txid
     * @param tranCallBack
     * @param callBackTimeOut 超时时间 -> 分钟
     */
    public TranCallBackInfo(String txid, TranCallBack tranCallBack, long callBackTimeOut) {
        this.txid = txid;
        this.tranCallBack = tranCallBack;
        this.callBackTimeOut = callBackTimeOut;
        this.callBackStart = System.currentTimeMillis();
    }

    /**
     * @return
     */
    public boolean isCallBackTimeOut() {
        return callBackTimePassed() > callBackTimeOut * 60;
    }

    /**
     * @return
     */
    public boolean isPullTimeOut() {
        if (this.lastPullTime == 0) {
            return callBackTimePassed() > pullInterval;
        } else {
            return (System.currentTimeMillis() / 1000 - this.lastPullTime / 1000) > pullInterval;
        }
    }

    public void onChain() {
        this.tranCallBack.isOnChain(txid, this.onChain);
    }

    /**
     * 回调过程中已经流逝的时间
     */
    private long callBackTimePassed() {
        return System.currentTimeMillis() / 1000 - this.callBackStart / 1000;
    }

    public String getTxid() {
        return txid;
    }

    public void setTxid(String txid) {
        this.txid = txid;
    }

    public long getLastPullTime() {
        return lastPullTime;
    }

    public void setLastPullTime(long lastPullTime) {
        this.lastPullTime = lastPullTime;
    }

    public boolean isOnChain() {
        return onChain;
    }

    public void setOnChain(boolean onChain) {
        this.onChain = onChain;
    }
}
