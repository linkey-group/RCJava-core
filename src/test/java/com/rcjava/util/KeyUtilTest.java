package com.rcjava.util;

import org.bouncycastle.openssl.PEMParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.PrivateKey;

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
}
