package com.rcjava.model;

import java.util.Map;

/**
 * @description:
 * @author: daiyongbing
 * @date: 2023/4/6
 * @version: 1.0
 */
public class Conclusion {
    private Long height;
    private String blockHash;
    private Map<String, String> illegalTrans;

    public Conclusion() {
    }

    public Long getHeight() {
        return height;
    }

    public void setHeight(Long height) {
        this.height = height;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public void setBlockHash(String blockHash) {
        this.blockHash = blockHash;
    }

    public Map<String, String> getIllegalTrans() {
        return illegalTrans;
    }

    public void setIllegalTrans(Map<String, String> illegalTrans) {
        this.illegalTrans = illegalTrans;
    }

    @Override
    public String toString() {
        return "Conclusion{" +
            "height=" + height +
            ", blockHash='" + blockHash + '\'' +
            ", illegalTrans=" + illegalTrans +
            '}';
    }
}
