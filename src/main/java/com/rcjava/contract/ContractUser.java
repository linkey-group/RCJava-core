package com.rcjava.contract;

import com.rcjava.protos.Peer;

import java.security.PrivateKey;

/**
 * 用户
 *
 * @author zyf
 */
public class ContractUser {

    /**
     * 用来标示账户和证书
     */
    private Peer.CertId certId;
    /**
     * 用户的私钥
     */
    private PrivateKey privateKey;
    /**
     * 签名算法，使用默认，和RepChain那边一致
     */
    private String signAlgorithm = "SHA256withECDSA";

    /**
     * 签名算法使用默认即可，如果RepChain那边有改动，这边可以set一个新的算法
     *
     * @param certId
     * @param privateKey
     */
    public ContractUser(Peer.CertId certId, PrivateKey privateKey) {
        this.certId = certId;
        this.privateKey = privateKey;
    }

    public Peer.CertId getCertId() {
        return certId;
    }

    public void setCertId(Peer.CertId certId) {
        this.certId = certId;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public String getSignAlgorithm() {
        return signAlgorithm;
    }

    public void setSignAlgorithm(String signAlgorithm) {
        this.signAlgorithm = signAlgorithm;
    }
}
