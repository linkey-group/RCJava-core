package com.rcjava.util;

import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.openssl.jcajce.JcaMiscPEMGenerator;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;

import java.io.*;
import java.security.PrivateKey;

/**
 * 主要是PEM相关操作工具类
 *
 * @author zyf
 */
public class PemUtil extends ProviderUtil {


    /**
     * 将privateKey转为pemString
     *
     * @param key                 PrivateKey
     * @param opensslLegacyFormat 是否是openssl格式, true：是， false：否（PKCS8）
     * @throws IOException
     */
    public static String toPemString(PrivateKey key, Boolean opensslLegacyFormat) throws IOException {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();
        JcaPEMWriter pemWriter = new JcaPEMWriter(new OutputStreamWriter(bOut));
        if (opensslLegacyFormat) {
            JcaMiscPEMGenerator misc = new JcaMiscPEMGenerator(key);
            pemWriter.writeObject(misc);
        } else {
            PKCS8Generator pkcs8 = new JcaPKCS8Generator(key, null);
            pemWriter.writeObject(pkcs8);
        }
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
     * 将私钥导出到PEM文件中
     *
     * @param pemFile             要输出的文件
     * @param privateKey          只能是私钥
     * @param opensslLegacyFormat 是否是openssl格式, true：是， false：否（PKCS8）
     * @throws IOException
     */
    public static void exportToPemFile(File pemFile, PrivateKey privateKey, Boolean opensslLegacyFormat) throws IOException {
        Writer writer = new FileWriter(pemFile);
        JcaPEMWriter pemWriter = new JcaPEMWriter(writer);
        if (opensslLegacyFormat) {
            JcaMiscPEMGenerator pemGenerator = new JcaMiscPEMGenerator(privateKey);
            pemWriter.writeObject(pemGenerator);
        } else {
            JcaPKCS8Generator pkcs8Generator = new JcaPKCS8Generator(privateKey, null);
            pemWriter.writeObject(pkcs8Generator);
        }
        pemWriter.close();
        writer.close();
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
