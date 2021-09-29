package com.rcjava.sign.impl;

import com.rcjava.sign.SignFunc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

/**
 * ECDSA 签名验签
 *
 * @author zyf
 */
public class ECDSASign implements SignFunc {

    /**
     * 默认算法
     */
    private String signAlgorithm = "SHA256withECDSA";
    private Logger logger = LoggerFactory.getLogger(getClass());

    public ECDSASign() {

    }

    public ECDSASign(String signAlgorithm) {
        this.signAlgorithm = signAlgorithm;
    }

    private ECDSASign(Builder builder) {
        setSignAlgorithm(builder.signAlgorithm);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * 用私钥进行签名
     *
     * @param privateKey 私钥
     * @param message    原始数据
     * @return
     */
    public byte[] sign(PrivateKey privateKey, byte[] message) {
        return this.sign(privateKey, message, signAlgorithm);
    }

    /**
     * 用私钥进行签名
     *
     * @param privateKey    私钥
     * @param message       原始数据
     * @param signAlgorithm 签名算法
     * @return
     */
    @Override
    public byte[] sign(PrivateKey privateKey, byte[] message, String signAlgorithm) {
        try {
            Signature s1 = Signature.getInstance(signAlgorithm);
            s1.initSign(privateKey);
            s1.update(message);
            return s1.sign();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            logger.error("签名出现错误：{}", e.getMessage(), e);
        }
        return new byte[0];
    }


    /**
     * 验证签名
     *
     * @param signature 签名数据
     * @param message   原始数据
     * @param publicKey 公钥
     * @return
     */
    public Boolean verify(byte[] signature, byte[] message, PublicKey publicKey) {
        return this.verify(signature, message, publicKey, signAlgorithm);
    }

    /**
     * 验证签名
     *
     * @param signature     签名数据
     * @param message       原始数据
     * @param publicKey     公钥
     * @param signAlgorithm 签名算法
     * @return
     */
    @Override
    public Boolean verify(byte[] signature, byte[] message, PublicKey publicKey, String signAlgorithm) {
        try {
            Signature s2 = Signature.getInstance(signAlgorithm);
            s2.initVerify(publicKey);
            s2.update(message);
            return s2.verify(signature);
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("验签出现错误：{}", ex.getMessage(), ex);
        }
        return false;
    }

    public String getSignAlgorithm() {
        return signAlgorithm;
    }

    public void setSignAlgorithm(String signAlgorithm) {
        this.signAlgorithm = signAlgorithm;
    }

    /**
     * {@code ECDSASign} builder static inner class.
     */
    public static final class Builder {
        private String signAlgorithm;

        private Builder() {
        }

        /**
         * Sets the {@code signAlgorithm} and returns a reference to this Builder so that the methods can be chained together.
         *
         * @param signAlgorithm the {@code signAlgorithm} to set
         * @return a reference to this Builder
         */
        public Builder setSignAlgorithm(String signAlgorithm) {
            this.signAlgorithm = signAlgorithm;
            return this;
        }

        /**
         * Returns a {@code ECDSASign} built from the parameters previously set.
         *
         * @return a {@code ECDSASign} built with parameters of this {@code ECDSASign.Builder}
         */
        public ECDSASign build() {
            return new ECDSASign(this);
        }
    }
}
