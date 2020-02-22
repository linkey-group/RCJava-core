package com.rcjava.util;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.util.io.pem.PemReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * 相关证书，私钥等操作工具类
 *
 * @author zyf
 */
public class CertUtil {

    private static Logger logger;

    static {
        Security.addProvider(new BouncyCastleProvider());
        logger = LoggerFactory.getLogger(CertUtil.class);
    }

    /**
     * @param jksFile  jks文件路径
     * @param password 密码
     * @param alias    别名
     * @return X509CertPrivateKey
     */
    public static X509CertPrivateKey genX509CertPrivateKey(File jksFile, String password, String alias) {
        try {
            FileInputStream fis = new FileInputStream(jksFile);
            X509CertPrivateKey x509CertPrivateKey = genX509CertPrivateKey(fis, password, alias);
            return x509CertPrivateKey;
        } catch (Exception e) {
            logger.error("从JKS文件中获取cert/key异常：{}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * @param jksStream jks输入流
     * @param password  密码
     * @param alias     别名
     * @return X509CertPrivateKey
     */
    public static X509CertPrivateKey genX509CertPrivateKey(InputStream jksStream, String password, String alias) {
        try {
            KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
            char[] pwd = password.toCharArray();
            store.load(jksStream, pwd);
            Key sk = store.getKey(alias, pwd);
            X509Certificate cert = (X509Certificate) store.getCertificate(alias);
            PrivateKey privateKey = (PrivateKey) sk;
            return new X509CertPrivateKey(cert, privateKey);
        } catch (Exception e) {
            logger.error("从JKS文件中获取cert/key异常：{}", e.getMessage(), e);
        } finally {
            // close the input stream
            try {
                if (jksStream != null) {
                    jksStream.close();
                }
            } catch (IOException e) {
                logger.error("Could not close input stream", e.getMessage(), e);
            }
        }
        return null;
    }

    /**
     * 获取本地证书，得到证书类和其序列化的字节序列
     *
     * @param certPath 证书路径
     * @return
     */
    public static X509Certificate readX509Cert(String certPath) {
        try {
            FileInputStream fis = new FileInputStream(certPath);
            X509Certificate x509Cert = readX509Cert(fis);
            return x509Cert;
        } catch (FileNotFoundException e) {
            logger.error("文件没找到", e.getMessage(), e);
        }
        return null;
    }

    /**
     * @param certStream 证书流
     * @return
     */
    public static X509Certificate readX509Cert(InputStream certStream) {
        try {
            CertificateFactory certF = CertificateFactory.getInstance("X.509");
            X509Certificate x509Cert = (X509Certificate) certF.generateCertificate(certStream);
            return x509Cert;
        } catch (CertificateException cfEx) {
            logger.error("根据certPath中构造x509证书异常：{}", cfEx.getMessage(), cfEx);
        } finally {
            if (certStream != null) {
                try {
                    certStream.close();
                } catch (IOException e) {
                    logger.error("Could not close input stream", e.getMessage(), e);
                }
            }
        }
        return null;
    }

    /**
     * 根据证书pem字符串，构造证书，construct certificate by pemString
     *
     * @param certPem cerPem是读取pem证书文件得到的字符串
     * @return X509Certificate
     * @throws Exception
     */
    public static X509Certificate generateX509CertByBC(String certPem) throws Exception {
        StringReader stringReader = new StringReader(certPem);
        PEMParser pemParser = new PEMParser(stringReader);
        Object object = pemParser.readObject();
        X509CertificateHolder x509Holder = (X509CertificateHolder) object;
        X509Certificate x509Cert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(x509Holder);
        pemParser.close();
        stringReader.close();
        return x509Cert;
    }

    /**
     * 根据证书pem字符串，构造证书，construct certificate by pemString
     *
     * @param certPem cerPem是读取pem证书文件得到的字符串
     * @return X509Certificate
     * @throws Exception
     */
    public static X509Certificate generateX509Cert(String certPem) throws Exception {
        StringReader stringReader = new StringReader(certPem);
        PemReader pemReader = new PemReader(stringReader);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        byte[] certByte = pemReader.readPemObject().getContent();
        X509Certificate x509Cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certByte));
        pemReader.close();
        stringReader.close();
        return x509Cert;
    }

    /**
     * certificate and privateKey
     */
    public static class X509CertPrivateKey {

        private X509Certificate x509Certificate;
        private PrivateKey privateKey;

        public X509CertPrivateKey(X509Certificate x509Certificate, PrivateKey privateKey) {
            this.x509Certificate = x509Certificate;
            this.privateKey = privateKey;
        }

        public X509Certificate getCertificate() {
            return x509Certificate;
        }

        public PrivateKey getPrivateKey() {
            return privateKey;
        }
    }
}
