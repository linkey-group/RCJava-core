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
     * 将privateKey转为pemString(PKCS8)
     *
     * @param key PrivateKey
     * @throws IOException
     */
    public static String toPKCS8PemString(PrivateKey key) throws IOException {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        JcaPEMWriter pemWriter = new JcaPEMWriter(new OutputStreamWriter(bOut));
        PKCS8Generator pkcs8 = new JcaPKCS8Generator(key, null);
        pemWriter.writeObject(pkcs8);
        pemWriter.close();
        bOut.close();
        return bOut.toString();
    }

    /**
     * 将Der编码转为PEM编码
     * convert byte[] to pemString
     *
     * @param type            类型，i.e., PRIVATE KEY、PUBLIC KEY、CERTIFICATE
     * @param encodeByteArray Der编码的字节数组
     * @return pem字符串
     * @throws IOException
     */
    public static String toPemString(String type, byte[] encodeByteArray) throws IOException {
        Writer writer = new StringWriter();
        JcaPEMWriter pemWriter = new JcaPEMWriter(writer);
        pemWriter.writeObject(new PemObject(type, encodeByteArray));
        pemWriter.close();
        writer.close();
        // pem字符串
        return writer.toString();
    }

    /**
     * 将Der编码转为PEM编码
     * 输出PEM字符串到文件中
     * export byte[] to File by pemString
     *
     * @param pemFile         文件路径
     * @param type            类型，PRIVATE KEY、PUBLIC KEY、CERTIFICATE
     * @param encodeByteArray Der编码的字节数组
     * @throws IOException
     */
    public static void exportToPemFile(File pemFile, String type, byte[] encodeByteArray) throws IOException {
        Writer writer = new FileWriter(pemFile);
        JcaPEMWriter pemWriter = new JcaPEMWriter(writer);
        pemWriter.writeObject(new PemObject(type, encodeByteArray));
        pemWriter.close();
        writer.close();
    }

    /**
     * 转为PEM字符串，PKCS1(if object instanceof PrivateKey == true)
     *
     * @param object 类型，i.e.,privateKey，publicKey，certificate
     * @return pem字符串
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
     * 输出PEM字符串到文件中，PKCS1(if object instanceof PrivateKey == true)
     *
     * @param pemFile 文件路径
     * @param object  类型，i.e.,privateKey，publicKey，certificate
     * @throws IOException
     */
    public static void exportToPemFile(File pemFile, Object object) throws IOException {
        Writer writer = new FileWriter(pemFile);
        JcaPEMWriter pemWriter = new JcaPEMWriter(writer);
        pemWriter.writeObject(object);
        pemWriter.close();
        writer.close();
    }

}
