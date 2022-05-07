package com.rcjava.util;

import com.rcjava.sign.impl.ECDSASign;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.jce.ECNamedCurveTable;

import java.io.File;
import java.io.FileOutputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;

/**
 * @author zyf
 */
public class GmUtilTest extends ProviderUtil {


    /**
     * 测试
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
            keyPairGenerator.initialize(new ECGenParameterSpec("sm2p256v1"));
//            keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"));
//            keyPairGenerator.initialize(ECNamedCurveTable.getParameterSpec("sm2p256v1"));
//            keyPairGenerator.initialize(ECNamedCurveTable.getParameterSpec("wapip192v1"));
            // 默认是使用secp256r1（prime256v1）
//            keyPairGenerator.initialize(256);
            KeyPair keypair1 = keyPairGenerator.generateKeyPair();
            byte[] signature = GmUtil.signWithSM2(keypair1.getPrivate(), "123".getBytes());
            boolean res = GmUtil.verifyWithSm2(keypair1.getPublic(), "123".getBytes(), signature);
            System.out.println(res);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // sm3摘要
        String sm3String = GmUtil.getHexStringWithSM3("123".getBytes());
        System.out.println(sm3String);

        // 生成国密密钥对
        KeyPair keyPair = GmUtil.getKeyPairWithSM2();
        PemUtil.exportToPemFile(new File("jks/test.kp"), keyPair);

        // 生成自签名证书
        X500Name x500Name = new X500NameBuilder(BCStyle.INSTANCE).addRDN(BCStyle.CN, "test").addRDN(BCStyle.OU, "iscas").addRDN(BCStyle.O, "RepChain").build();
        X509Certificate x509Certificate = CertUtil.createX509Certificate(x500Name, keyPair, "SM3withSM2", 24 * 365 * 5);
        x509Certificate.checkValidity();
        x509Certificate.verify(keyPair.getPublic());
        System.out.println(x509Certificate);
        new FileOutputStream("jks/test.der").write(x509Certificate.getEncoded());

        // 保存证书, BKS 或者 pfx
        KeyStore keyStore = KeyStore.getInstance("BKS", "BC");
        keyStore.load(null, null);
        keyStore.setKeyEntry("test", keyPair.getPrivate(), "123".toCharArray(), new Certificate[]{x509Certificate});
        keyStore.store(new FileOutputStream("jks/test.bks"), "123".toCharArray());

        // 签名验签
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        byte[] input = "1234567890".getBytes();
        byte[] signature = GmUtil.signWithSM2(privateKey, input);
        boolean res = GmUtil.verifyWithSm2(publicKey, input, signature);
        System.out.println(res);

        boolean res_1 = new ECDSASign("SM3withSM2").verify(signature, input, publicKey);
        System.out.println(res_1);

        // 转pemString-pkcs8
        String privateKeyPemPkcs8String = PemUtil.toPKCS8PemString(privateKey);
        // 转pemString-openssl
        String privateKeyPemString = PemUtil.toPemString(privateKey);

    }

}
