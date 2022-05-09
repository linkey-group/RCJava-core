package com.rcjava.util;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.security.cert.X509Certificate;

/**
 * @author zyf
 */
public class CertUtilTest {

    @Test
    @DisplayName("测试读取证书")
    void testGenerateCert() throws Exception {
        X509Certificate cert = CertUtil.generateX509Cert(new File(("jks/jdk13/node1.cer")));

        String cert_str = new String(Files.readAllBytes(new File(("jks/jdk13/node1.cer")).toPath()));
        X509Certificate cert_1 = CertUtil.generateX509Cert(cert_str);

        System.out.println(cert);
        System.out.println(cert_1);

        Assertions.assertSame(cert, cert_1);
        Assertions.assertEquals(cert.getSerialNumber(), cert_1.getSerialNumber(), "序列号一致");
    }
}
