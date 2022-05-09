package com.rcjava.util;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.openssl.*;
import org.bouncycastle.openssl.jcajce.*;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.util.io.pem.PemGenerationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.security.*;

/**
 * 该类以后主要放key相关的工具
 *
 * @author zyf
 */
public class KeyUtil extends ProviderUtil {

    private static Logger keyLogger = LoggerFactory.getLogger(KeyUtil.class);


    /**
     * convert privateKey to pem and encrypt, 用法请参考KeyUtilTest.java
     *
     * @param privateKeyPEMWriter Wrap StringWriter or FileWriter
     * @param privateKey          私钥
     * @param opensslLegacyFormat 是否是标准openssl格式,true:pkcs1, false:pkcs8
     * @param encryptAlg          算法: 如果是PKCS8格式{@link JceOpenSSLPKCS8EncryptorBuilder#AES_256_CBC}，如果是openssl格式，请查阅：<openssl list-cipher-algorithms> or < openssl enc -help>, 注意要大写
     * @param pass                密码
     * @throws IOException
     * @throws OperatorCreationException
     * @throws PemGenerationException
     */
    public static void generateEncryptPemString(@Nonnull JcaPEMWriter privateKeyPEMWriter, @Nonnull PrivateKey privateKey, @Nonnull Boolean opensslLegacyFormat, @Nullable String encryptAlg, @Nullable String pass)
            throws IOException, OperatorCreationException, PemGenerationException {
        if (null != encryptAlg && "" != encryptAlg) {
            if (!opensslLegacyFormat) {
                JceOpenSSLPKCS8EncryptorBuilder encryptorBuilder = new JceOpenSSLPKCS8EncryptorBuilder(new ASN1ObjectIdentifier(encryptAlg));
                encryptorBuilder.setProvider("BC");
                encryptorBuilder.setPasssword(pass.toCharArray());
                encryptorBuilder.setPRF(PKCS8Generator.PRF_HMACSHA1);
                PKCS8Generator pkcs8 = new JcaPKCS8Generator(privateKey, encryptorBuilder.build());
                privateKeyPEMWriter.writeObject(pkcs8);

            } else {
                JcePEMEncryptorBuilder encryptorBuilder = new JcePEMEncryptorBuilder(encryptAlg);
                encryptorBuilder.setProvider("BC");
                encryptorBuilder.setSecureRandom(new SecureRandom());
                PEMEncryptor encryptor = encryptorBuilder.build(pass.toCharArray());
                MiscPEMGenerator pkcs1 = new JcaMiscPEMGenerator(privateKey, encryptor);
                privateKeyPEMWriter.writeObject(pkcs1);
            }
        } else {
            if (!opensslLegacyFormat) {
                PKCS8Generator pkcs8 = new JcaPKCS8Generator(privateKey, null);
                privateKeyPEMWriter.writeObject(pkcs8);
            } else {
                MiscPEMGenerator pkcs1 = new JcaMiscPEMGenerator(privateKey);
                privateKeyPEMWriter.writeObject(pkcs1);
            }
        }
        privateKeyPEMWriter.flush();

    }

    /**
     * decrypt priPemString and convert it to privateKey
     *
     * @param privateKeyPEMParser 用来读取pem文件
     * @param pass                密码, 如果没有密码则 设置为空字符串 ""
     * @return PrivateKey
     * @throws IOException
     * @throws OperatorCreationException
     * @throws PKCSException
     */
    public static PrivateKey generatePrivateKey(@Nonnull PEMParser privateKeyPEMParser, @Nullable String pass)
            throws IOException, OperatorCreationException, PKCSException {
        Object object = null;
        try {
            object = privateKeyPEMParser.readObject();
            if (null == object) {
                keyLogger.error("Can not load the private key: no input data");
                throw new IOException("Can not load the private key: no input data");
            }
        } catch (IOException ioEx) {
            keyLogger.error("Can not load the private key: {}", ioEx);
            throw new IOException(String.format("Can not load the private key: %s", ioEx));
        }
        if (object instanceof PEMKeyPair) {
            PEMKeyPair kp = (PEMKeyPair) object;
            return new JcaPEMKeyConverter().setProvider("BC").getPrivateKey(kp.getPrivateKeyInfo());
        } else if (object instanceof PrivateKeyInfo) {
            return new JcaPEMKeyConverter().setProvider("BC").getPrivateKey((PrivateKeyInfo) object);
        } else if (object instanceof PEMEncryptedKeyPair) {
            JcePEMDecryptorProviderBuilder provBuilder = new JcePEMDecryptorProviderBuilder().setProvider("BC");
            PEMDecryptorProvider decProvider = provBuilder.build(pass.toCharArray());
            PEMKeyPair kp = ((PEMEncryptedKeyPair) object).decryptKeyPair(decProvider);
            return new JcaPEMKeyConverter().setProvider("BC").getPrivateKey(kp.getPrivateKeyInfo());
        } else if (object instanceof PKCS8EncryptedPrivateKeyInfo) {
            JceOpenSSLPKCS8DecryptorProviderBuilder proBuilder = new JceOpenSSLPKCS8DecryptorProviderBuilder().setProvider("BC");
            InputDecryptorProvider decProv = proBuilder.build(pass.toCharArray());
            PKCS8EncryptedPrivateKeyInfo pInfo = (PKCS8EncryptedPrivateKeyInfo) object;
            return new JcaPEMKeyConverter().setProvider("BC").getPrivateKey(pInfo.decryptPrivateKeyInfo(decProv));
        } else {
            keyLogger.error("Can not load the private key");
            throw new IOException("Can not load the private key, unknown format");
        }
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
