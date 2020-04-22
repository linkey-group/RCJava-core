package com.rcjava.sync;

/**
 * 同步信息临时缓存值
 *
 * @author zyf
 */
public final class SyncInfo {

    /**
     * 当前块高度，已同步的最新块的高度
     */
    private volatile long localHeight;
    /**
     * 当前块Hash，已同步的本地最新块Hash
     */
    private volatile String locBlkHash;
//    private volatile String preBlkHash = "";

    public SyncInfo(long localHeight, String locBlkHash) {
        this.localHeight = localHeight;
        this.locBlkHash = locBlkHash;
    }

    public long getLocalHeight() {
        return localHeight;
    }

    public void setLocalHeight(long localHeight) {
        this.localHeight = localHeight;
    }

    public String getLocBlkHash() {
        return locBlkHash;
    }

    public void setLocBlkHash(String locBlkHash) {
        this.locBlkHash = locBlkHash;
    }

    @Override
    public String toString() {
        return "SyncInfo{" +
                "localHeight=" + localHeight +
                ", locBlkHash='" + locBlkHash + '\'' +
                '}';
    }
}
