package com.rcjava.util;

import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.jcajce.spec.SM2ParameterSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.*;
import java.security.spec.ECGenParameterSpec;

/**
 * 使用国密算法生成密钥对、证书、签名验签；默认使用BouncyCastle
 *
 * @author zyf
 */
public class GmUtil extends ProviderUtil{

    private static Logger logger = LoggerFactory.getLogger(GmUtil.class);

    /**
     * 计算sm3摘要
     *
     * @param input 需要计算摘要的数据
     * @return
     */
    public static byte[] getBytesWithSM3(byte[] input) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SM3", "BC");
            return messageDigest.digest(input);
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            logger.error("计算SM3摘要错误：{}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 计算sm3摘要
     *
     * @param input 需要计算摘要的数据
     * @return
     */
    public static String getHexStringWithSM3(byte[] input) {
        byte[] digestRes = getBytesWithSM3(input);
        if (digestRes != null) {
            return Hex.encodeHexString(digestRes);
        }
        return null;
    }

    /**
     * 生成sm2密钥对，使用sm2p256v1
     *
     * @return
     */
    public static KeyPair getKeyPairWithSM2() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
            keyPairGenerator.initialize(new ECGenParameterSpec("sm2p256v1"));
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
            logger.error("生成SM2密钥对错误：{}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 使用SM3withSM2签名
     *
     * @param privateKey sm2生成的私钥
     * @param input      原始数据
     * @return
     */
    public static byte[] signWithSM2(PrivateKey privateKey, byte[] input) {
        try {
            Signature sigEngine = Signature.getInstance("SM3withSM2", "BC");
            sigEngine.initSign(privateKey);
            sigEngine.update(input);
            return sigEngine.sign();
        } catch (NoSuchAlgorithmException | SignatureException | NoSuchProviderException | InvalidKeyException e) {
            logger.error("使用SM3withSM2签名错误：{}", e.getMessage(), e);
        }
        return new byte[0];
    }

    /**
     * 使用SM3withSM2签名
     *
     * @param privateKey       sm2生成的私钥
     * @param input            原始数据
     * @param sm2ParameterSpec ID参数
     * @return
     */
    public static byte[] signWithSM2(PrivateKey privateKey, byte[] input, SM2ParameterSpec sm2ParameterSpec) {
        try {
            Signature sigEngine = Signature.getInstance("SM3withSM2", "BC");
            sigEngine.setParameter(sm2ParameterSpec);
            sigEngine.initSign(privateKey);
            sigEngine.update(input);
            return sigEngine.sign();
        } catch (NoSuchAlgorithmException | SignatureException | NoSuchProviderException | InvalidKeyException | InvalidAlgorithmParameterException e) {
            logger.error("使用SM3withSM2签名错误：{}", e.getMessage(), e);
        }
        return new byte[0];
    }

    /**
     * 验证SM3withSM2签名
     *
     * @param publicKey sm2生成的公钥
     * @param input     原始数据
     * @param signature 签名数据
     * @return
     */
    public static boolean verifyWithSm2(PublicKey publicKey, byte[] input, byte[] signature) {
        try {
            Signature sigEngine = Signature.getInstance("SM3withSM2", "BC");
            sigEngine.initVerify(publicKey);
            sigEngine.update(input);
            return sigEngine.verify(signature);
        } catch (NoSuchAlgorithmException | SignatureException | NoSuchProviderException | InvalidKeyException e) {
            logger.error("验证SM3withSM2签名错误：{}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * 验证SM3withSM2签名
     *
     * @param publicKey        sm2生成的公钥
     * @param input            原始数据
     * @param signature        签名数据
     * @param sm2ParameterSpec ID参数
     * @return
     */
    public static boolean verifyWithSm2(PublicKey publicKey, byte[] input, byte[] signature, SM2ParameterSpec sm2ParameterSpec) {
        try {
            Signature sigEngine = Signature.getInstance("SM3withSM2", "BC");
            sigEngine.setParameter(sm2ParameterSpec);
            sigEngine.initVerify(publicKey);
            sigEngine.update(input);
            return sigEngine.verify(signature);
        } catch (NoSuchAlgorithmException | SignatureException | NoSuchProviderException | InvalidKeyException | InvalidAlgorithmParameterException e) {
            logger.error("验证SM3withSM2签名错误：{}", e.getMessage(), e);
        }
        return false;
    }

}
