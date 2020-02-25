package com.rcjava.util;

import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.PEMUtil;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.util.io.pem.PemObject;
import org.junit.After;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.security.Key;
import java.security.PrivateKey;
import java.util.Objects;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author zyf
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PemUtilTest {

    private PrivateKey privateKey;

    @BeforeAll
    void readPrivateKey() {
        privateKey = Objects.requireNonNull(CertUtil.genX509CertPrivateKey(
                new File("jks/121000005l35120456.node1.jks"),
                "123",
                "121000005l35120456.node1"), "获取证书和私钥失败").getPrivateKey();
    }

    @Test
    @DisplayName("export to pemFile")
    void testExportToPemFile() throws IOException {


        PemUtil.exportToPemFile(new File("jks/pri_pkcs1.pem"), privateKey);

        // PKCS8 Der编码
        byte[] pri = privateKey.getEncoded();
        PemUtil.exportToPemFile(new File("jks/pri_pkcs8.pem"), "PRIVATE KEY", privateKey.getEncoded());

        // privateKeyInfo PKCS8的格式
        PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(pri);
        // privateKeyInfo.parsePrivateKey()拿到ECPrivateKey的编码
        ASN1Primitive asn1Primitive = privateKeyInfo.parsePrivateKey().toASN1Primitive();
        // PKCS1 Der编码
        byte[] pri_2 = asn1Primitive.getEncoded();

        assertThat(pri).isNotEqualTo(pri_2);

        System.out.println(PemUtil.toPemString("EC PRIVATE KEY", pri_2));

        // PKCS1，privateKey最终转为了privateKeyInfo
        assertThat(PemUtil.toPemString(privateKey))
                .isEqualTo(PemUtil.toPemString("EC PRIVATE KEY", pri_2));

        // PKCS1
        assertThat(PemUtil.toPemString(privateKeyInfo))
                .isEqualTo(PemUtil.toPemString("EC PRIVATE KEY", pri_2));

        assertThat(PemUtil.toPemString(privateKeyInfo))
                .isEqualTo(PemUtil.toPemString(privateKey));

    }

    @Test
    @DisplayName("convert to pemString")
    void testToPemString() throws IOException {

        String pri_pkcs1 = PemUtil.toPemString(privateKey);
        String pri_pkcs8 = PemUtil.toPemString("PRIVATE KEY", privateKey.getEncoded());

        assertThat(pri_pkcs1).isNotEqualTo(pri_pkcs8);

        System.out.println(pri_pkcs1);
        System.out.println(pri_pkcs8);

    }

    @AfterAll
    void deleteFile() {
        new File("jks/pri_pkcs1.pem").deleteOnExit();
        new File("jks/pri_pkcs8.pem").deleteOnExit();
    }

}
