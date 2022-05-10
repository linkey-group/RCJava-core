package com.rcjava.util;

import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * @author zyf
 */
public class KeyUtilTest<T> {

    PrivateKey generatePrivateKey(T pem, String password) throws Exception {
        PrivateKey privateKey = null;
        if (pem instanceof String) {
            PEMParser stringParser = new PEMParser(new StringReader((String) pem));
            privateKey = KeyUtil.generatePrivateKey(stringParser, password);
        } else if (pem instanceof File) {
            PEMParser fileParser = new PEMParser(new FileReader((File) pem));
            privateKey = KeyUtil.generatePrivateKey(fileParser, password);
        }
        System.out.println(privateKey);
        return privateKey;
    }

    @Test
    @DisplayName("使用StringReader, 测试从PemString获取私钥")
    void testString() throws Exception {
        byte[] bytes = Files.readAllBytes(new File("jks/jdk13/node1.key").toPath());
        String pemString = new String(bytes, StandardCharsets.UTF_8);
        PrivateKey privateKey = new KeyUtilTest<String>().generatePrivateKey(pemString, "123");
        System.out.println(privateKey);
    }

    @Test
    @DisplayName("使用FileReader, 测试从PemFile获取私钥")
    void testFile() throws Exception {
        File pemFile = new File("jks/jdk13/node1.pkcs8");
        PrivateKey privateKey = new KeyUtilTest<File>().generatePrivateKey(pemFile, "123");
        System.out.println(privateKey);
    }

    @Test
    @DisplayName("测试使用私钥生成公钥")
    void testGeneratePublicKey() throws Exception {
        File pemFile = new File("jks/jdk13/node1.pkcs8");
        PrivateKey privateKey = new KeyUtilTest<File>().generatePrivateKey(pemFile, "123");
        KeyPair keyPair = KeyUtil.generateKeyPair(privateKey);
        Assertions.assertArrayEquals(privateKey.getEncoded(), keyPair.getPrivate().getEncoded());
        PublicKey publicKey = keyPair.getPublic();
        System.out.println(publicKey);
    }

    @Test
    @DisplayName("生成加密Pem字符串, 如果加密算法为空, 则生成不加密字符串")
    void testGenerateEncryptPemString() throws Exception {
        File pemFile = new File("jks/jdk13/node1.pkcs8");
        PrivateKey privateKey = new KeyUtilTest<File>().generatePrivateKey(pemFile, "123");
        JcaPEMWriter pemWriter = new JcaPEMWriter(new FileWriter("jks/jdk13/encryptPem.key"));
        // PKCS8
        KeyUtil.generateEncryptPemString(pemWriter, privateKey, false, JceOpenSSLPKCS8EncryptorBuilder.AES_256_CBC, "123");
        // openssl
        KeyUtil.generateEncryptPemString(pemWriter, privateKey, true, "AES-256-CBC", "123");
        pemWriter.close();
    }
}
