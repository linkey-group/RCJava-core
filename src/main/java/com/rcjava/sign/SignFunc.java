package com.rcjava.sign;

import java.security.PrivateKey;
import java.security.PublicKey;

public interface SignFunc {

    /**
     * @param privateKey    私钥
     * @param message       原始数据
     * @param signAlgorithm 签名算法
     * @return
     */
    byte[] sign(PrivateKey privateKey, byte[] message, String signAlgorithm);

    /**
     * @param signature     签名信息
     * @param message       不带签名的message
     * @param publicKey     公钥
     * @param signAlgorithm 签名算法
     * @return
     */
    Boolean verify(byte[] signature, byte[] message, PublicKey publicKey, String signAlgorithm);
}
