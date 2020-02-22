package com.rcjava.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.util.io.pem.PemObject;

import java.io.*;
import java.security.PrivateKey;
import java.security.Security;

/**
 * 主要是PEM相关操作工具类
 *
 * @author zyf
 */
public class PemUtil {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * 将byte[]转为PEM字符串      pkcs8(私钥)
     * convert byte[] to pemString
     *
     * @param type      类型，PRIVATE KEY、PUBLIC KEY、CERTIFICATE
     * @param byteArray
     * @return
     * @throws IOException
     */
    public static String toPemString(String type, byte[] byteArray) throws IOException {
        Writer writer = new StringWriter();
        JcaPEMWriter pemWriter = new JcaPEMWriter(writer);
        pemWriter.writeObject(new PemObject(type, byteArray));
        String pemString = writer.toString();
        pemWriter.flush();
        pemWriter.close();
        writer.close();
        // pem字符串
        return pemString;
    }

    /**
     * 转为pemString             PKCS1(如果是私钥)
     *
     * @param object 只能privateKey，publicKey，certificate
     * @return
     * @throws IOException
     */
    public static String toPemString(Object object) throws IOException {
        Writer writer = new StringWriter();
        JcaPEMWriter pemWriter = new JcaPEMWriter(writer);
        pemWriter.writeObject(object);
        pemWriter.close();
        writer.close();
        // pem字符串
        return writer.toString();
    }

    /**
     * 将privateKey转为pemString(PKCS8)
     *
     * @param key PrivateKey
     * @throws IOException
     */
    public static String toPkcs8PemString(PrivateKey key) throws IOException {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        JcaPEMWriter pWrt = new JcaPEMWriter(new OutputStreamWriter(bOut));
        PKCS8Generator pkcs8 = new JcaPKCS8Generator(key, null);
        pWrt.writeObject(pkcs8);
        pWrt.close();
        return bOut.toString();
    }

    /**
     * 输出PEM字符串到文件中     PKCS1(如果是私钥)
     *
     * @param path   文件路径
     * @param object 只能privateKey，publicKey，certificate
     * @throws IOException
     */
    public static void exportToPemFile(String path, Object object) throws IOException {
        File file = new File(path);
        Writer writer = new FileWriter(file);
        JcaPEMWriter pemWriter = new JcaPEMWriter(writer);
        pemWriter.writeObject(object);
        pemWriter.close();
        writer.close();
    }

    /**
     * 输出PEM字符串到文件中     PKCS8(如果是私钥)
     * export byte[] to File by pemString
     *
     * @param path      文件路径
     * @param type      类型，PRIVATE KEY、PUBLIC KEY、CERTIFICATE
     * @param byteArray
     * @throws IOException
     */
    public static void exportToPemFile(String path, String type, byte[] byteArray) throws IOException {
        File file = new File(path);
        Writer writer = new FileWriter(file);
        JcaPEMWriter pemWriter = new JcaPEMWriter(writer);
        pemWriter.writeObject(new PemObject(type, byteArray));
        pemWriter.close();
        writer.close();
    }

}
