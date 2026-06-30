package com.rcjava.sign.impl;

import com.rcjava.sign.SignFunc;
import com.rcjava.util.ProviderUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;

/**
 * PQC 签名验签，当前用于 ML-DSA。
 */
public class PqcSign extends ProviderUtil implements SignFunc {

    private String signAlgorithm = "ML-DSA-65";
    private Logger logger = LoggerFactory.getLogger(getClass());

    public PqcSign() {
    }

    public PqcSign(String signAlgorithm) {
        this.signAlgorithm = signAlgorithm;
    }

    public byte[] sign(PrivateKey privateKey, byte[] message) {
        return this.sign(privateKey, message, signAlgorithm);
    }

    @Override
    public byte[] sign(PrivateKey privateKey, byte[] message, String signAlgorithm) {
        try {
            Signature s = Signature.getInstance(signAlgorithm.trim(), BouncyCastleProvider.PROVIDER_NAME);
            s.initSign(privateKey);
            s.update(message);
            return s.sign();
        } catch (Exception e) {
            logger.error("PQC签名出现错误：{}", e.getMessage(), e);
        }
        return new byte[0];
    }

    public Boolean verify(byte[] signature, byte[] message, PublicKey publicKey) {
        return this.verify(signature, message, publicKey, signAlgorithm);
    }

    @Override
    public Boolean verify(byte[] signature, byte[] message, PublicKey publicKey, String signAlgorithm) {
        try {
            Signature s = Signature.getInstance(signAlgorithm.trim(), BouncyCastleProvider.PROVIDER_NAME);
            s.initVerify(publicKey);
            s.update(message);
            return s.verify(signature);
        } catch (Exception e) {
            logger.error("PQC验签出现错误：{}", e.getMessage(), e);
        }
        return false;
    }

    public String getSignAlgorithm() {
        return signAlgorithm;
    }

    public void setSignAlgorithm(String signAlgorithm) {
        this.signAlgorithm = signAlgorithm;
    }
}