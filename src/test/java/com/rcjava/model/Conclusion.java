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
    private String hash_tx;
    private Map<String, String> illegal_txIds;

    public Conclusion() {
    }

    public Conclusion(Long height, String hash_tx, Map<String, String> illegal_txIds) {
        this.height = height;
        this.hash_tx = hash_tx;
        this.illegal_txIds = illegal_txIds;
    }

    public Long getHeight() {
        return height;
    }

    public void setHeight(Long height) {
        this.height = height;
    }

    public String getHash_tx() {
        return hash_tx;
    }

    public void setHash_tx(String hash_tx) {
        this.hash_tx = hash_tx;
    }

    public Map<String, String> getIllegal_txIds() {
        return illegal_txIds;
    }

    public void setIllegal_txIds(Map<String, String> illegal_txIds) {
        this.illegal_txIds = illegal_txIds;
    }

    @Override
    public String toString() {
        return "Conclusion{" +
            "height=" + height +
            ", hash_tx='" + hash_tx + '\'' +
            ", illegal_txIds=" + illegal_txIds +
            '}';
    }
}
