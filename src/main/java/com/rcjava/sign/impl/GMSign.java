package com.rcjava.sign.impl;

import com.rcjava.sign.SignFunc;
import com.rcjava.util.GmUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * @author zyf
 */
public class GMSign implements SignFunc {

    /**
     * 默认算法
     */
    private String signAlgorithm = "SM3withSM2";
    private Logger logger = LoggerFactory.getLogger(getClass());

    public GMSign() {
    }

    public GMSign(String signAlgorithm) {
        this.signAlgorithm = signAlgorithm;
    }

    /**
     * @param privateKey    私钥
     * @param message       原始数据
     * @return
     */
    public byte[] sign(PrivateKey privateKey, byte[] message) {
        return GmUtil.signWithSM2(privateKey, message);
    }

    /**
     * @param privateKey    私钥
     * @param message       原始数据
     * @param signAlgorithm 签名算法
     * @return
     */
    @Override
    public byte[] sign(PrivateKey privateKey, byte[] message, String signAlgorithm) {
        if (signAlgorithm.equalsIgnoreCase(this.signAlgorithm)) {
            return GmUtil.signWithSM2(privateKey, message);
        } else {
            logger.error(String.format("国密暂时不支持该签名算法: %s", signAlgorithm));
        }
        return new byte[0];
    }

    /**
     * @param signature     签名信息
     * @param message       不带签名的message
     * @param publicKey     公钥
     * @return
     */
    public Boolean verify(byte[] signature, byte[] message, PublicKey publicKey) {
        return GmUtil.verifyWithSm2(publicKey, message, signature);
    }

    /**
     * @param signature     签名信息
     * @param message       不带签名的message
     * @param publicKey     公钥
     * @param signAlgorithm 签名算法
     * @return
     */
    @Override
    public Boolean verify(byte[] signature, byte[] message, PublicKey publicKey, String signAlgorithm) {
        if (signAlgorithm.equalsIgnoreCase(this.signAlgorithm)) {
            return GmUtil.verifyWithSm2(publicKey, message, signature);
        } else {
            logger.error(String.format("国密暂时不支持该验签算法: %s", signAlgorithm));
        }
        return false;
    }

    public String getSignAlgorithm() {
        return signAlgorithm;
    }

}
