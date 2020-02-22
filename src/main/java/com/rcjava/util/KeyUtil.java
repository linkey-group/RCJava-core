package com.rcjava.util;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;

/**
 * 该类以后主要放key相关的工具
 *
 * @author zyf
 */
public class KeyUtil {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }


    /**
     * 暂时不支持设置SecureRandom与iterationCount
     * convert privateKey to pem and encrypt
     *
     * @param privateKey 私钥
     * @param pass       密码
     * @return encryptPrivateKeyPem
     * @throws Exception
     */
    public static String generateEncryptPemString(@Nonnull PrivateKey privateKey, String pass) throws Exception {
        JceOpenSSLPKCS8EncryptorBuilder encryptorBuilder = new JceOpenSSLPKCS8EncryptorBuilder(JcaPKCS8Generator.DES3_CBC);
        encryptorBuilder.setProvider("BC");
        encryptorBuilder.setPasssword(pass.toCharArray());
        encryptorBuilder.setPRF(JcaPKCS8Generator.PRF_HMACSHA1);
        PKCS8Generator pkcs8 = new JcaPKCS8Generator(privateKey, encryptorBuilder.build());
        return PemUtil.toPemString(pkcs8);
    }

    /**
     * decrypt priPemString and convert it to privateKey
     *
     * @param priPemString priPem是pem文件路径或读取pem私钥文件得到的字符串
     * @param pass         密码
     * @return PrivateKey
     * @throws Exception
     */
    public static PrivateKey generatePrivateKey(@Nonnull String priPemString, String pass) throws Exception {
        PEMParser pemParser = new PEMParser(new StringReader(priPemString));
        Object object = pemParser.readObject();
        PrivateKey privateKey;
        if (null == pass || "".equals(pass.trim())) {
            if (object instanceof PEMKeyPair) {
                PEMKeyPair kp = (PEMKeyPair) object;
                privateKey = new JcaPEMKeyConverter().setProvider("BC").getPrivateKey(kp.getPrivateKeyInfo());
            } else if (object instanceof PrivateKeyInfo) {
                privateKey = new JcaPEMKeyConverter().setProvider("BC").getPrivateKey((PrivateKeyInfo) object);
            } else {
                throw new IOException("unrecognised private key pemFile or pemString");
            }
        } else {
            InputDecryptorProvider decProv = new JceOpenSSLPKCS8DecryptorProviderBuilder().setProvider("BC").build(pass.toCharArray());
            PKCS8EncryptedPrivateKeyInfo pInfo = (PKCS8EncryptedPrivateKeyInfo) object;
            privateKey = new JcaPEMKeyConverter().setProvider("BC").getPrivateKey(pInfo.decryptPrivateKeyInfo(decProv));
        }
        return privateKey;
    }

    /**
     * 根据给定的私钥返回公钥
     * {@link #getPublicKeyFromPrivateKey(PrivateKey) "均利用了BC，二者实现略微不同"}
     *
     * @param bcecPrivateKey
     * @return PublicKey
     * @throws Exception
     */
    public static PublicKey getPublicKeyFromPrivateKey(@Nonnull BCECPrivateKey bcecPrivateKey) throws Exception {
        ECParameterSpec ecSpec = bcecPrivateKey.getParameters();
        ECPoint Q = ecSpec.getG().multiply(bcecPrivateKey.getD());
        byte[] publicDerBytes = Q.getEncoded(false);
        ECPoint point = ecSpec.getCurve().decodePoint(publicDerBytes);
        ECPublicKeySpec pubSpec = new ECPublicKeySpec(point, ecSpec);
        KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
        return keyFactory.generatePublic(pubSpec);
    }

    /**
     * 根据给定的私钥返回公钥
     * {@link #getPublicKeyFromPrivateKey(BCECPrivateKey) "均利用了BC，二者实现略微不同"}
     *
     * @param privateKey
     * @return PublicKey
     * @throws Exception
     */
    public static PublicKey getPublicKeyFromPrivateKey(@Nonnull PrivateKey privateKey) throws Exception {
        ECPrivateKey ecPrivateKey = (ECPrivateKey) privateKey;
        ECParameterSpec ecSpec = ecPrivateKey.getParameters();
        ECPoint Q = ecSpec.getG().multiply(ecPrivateKey.getD());
        ECPublicKeySpec pubSpec = new ECPublicKeySpec(Q, ecSpec);
        KeyFactory keyFactory = KeyFactory.getInstance("EC", "BC");
        return keyFactory.generatePublic(pubSpec);
    }

    /**
     * 通过privateKey生成PEMKeyPair，进而可以获得publicKey
     *
     * @param privateKey PrivateKey实例
     * @return
     * @throws Exception
     */
    public static PEMKeyPair generateKPbyPrivateKey(@Nonnull PrivateKey privateKey) throws Exception {
        // pkcs1
        String priPemString = PemUtil.toPemString(privateKey);
        PEMParser pemParser = new PEMParser(new StringReader(priPemString));
        PEMKeyPair pemKeyPair = (PEMKeyPair) pemParser.readObject();
        return pemKeyPair;
    }

}
