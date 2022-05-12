package com.rcjava.did;

import com.rcjava.client.ChainInfoClient;
import com.rcjava.client.TranPostClient;
import com.rcjava.protos.Peer;
import com.rcjava.tran.TranCreator;
import com.rcjava.util.CertUtil;
import com.rcjava.util.KeyUtil;
import com.rcjava.util.PemUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCSException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author zyf
 */
public class DidTest {

    String host = "localhost:9081";
    TranPostClient postClient = new TranPostClient(host);
    ChainInfoClient infoClient = new ChainInfoClient(host);

    Peer.ChaincodeId contractAssetsId = Peer.ChaincodeId.newBuilder().setChaincodeName("ContractAssetsTPL").setVersion(1).build();
    Peer.ChaincodeId chaincodeId = Peer.ChaincodeId.newBuilder().setChaincodeName("RdidOperateAuthorizeTPL").setVersion(1).build();

    static String signUpSigner = "signUpSigner";
    static String updateSigner = "updateSigner";
    static String updateSignerStatus = "updateSignerStatus";

    static String signUpCertificate = "signUpCertificate"; // 无需授权
    static String updateCertificateStatus = "updateCertificateStatus"; // 无需授权
    static String signUpAllTypeCertificate = "signUpAllTypeCertificate"; // 需授权
    static String updateAllTypeCertificateStatus = "updateAllTypeCertificateStatus"; // 需授权

    static String grantOperate = "grantOperate";
    static String updateGrantOperateStatus = "updateGrantOperateStatus";
    static String bindCertToAuthorize = "bindCertToAuthorize";

    static String signUpOperate = "signUpOperate";
    static String updateOperateStatus = "updateOperateStatus";

    public DidTest() throws IOException {
    }

    public static void main(String[] args) throws Exception {
        genCertPriKeyJks();
    }

    /**
     * 生成证书和私钥
     *
     * @throws Exception
     */
    static void genCertPriKeyJks() throws Exception {
        for (int i = 0; i <= 3; i++) {
            for (int j = 0; j < 2; j++) {
                String usr_alias = "usr_" + i + "_" + j;
                CertUtil.genJksFile(new File("jks/did/" + usr_alias + ".jks"), usr_alias, "123");
            }
        }
        for (int i = 0; i <= 3; i++) {
            for (int j = 0; j < 2; j++) {
                String usr_alias = "usr_" + i + "_" + j;
                CertUtil.X509CertPrivateKey certPrivateKey = CertUtil.genX509CertPrivateKey(new File("jks/did/" + usr_alias + ".jks"), "123", usr_alias);
                PemUtil.exportToPemFile(new File("jks/did/" + usr_alias + ".cer"), certPrivateKey.getCertificate());
                PemUtil.exportToPemFile(new File("jks/did/" + usr_alias + ".pkcs8"), certPrivateKey.getPrivateKey(), false);
            }
        }
    }

    /**
     * @param CreditCode 信用码
     * @param certPem    证书pem字符串
     * @param certName   证书名
     * @return
     * @throws Exception
     */
    SignerCert genCertSigner(String CreditCode, String certPem, String certName) {
        Peer.Certificate cert_proto = Peer.Certificate.newBuilder()
                .setCertificate(certPem)
                .setAlgType("sha256withecdsa")
                .setCertValid(true)
                .setCertType(Peer.Certificate.CertType.CERT_AUTHENTICATION)
                .setId(Peer.CertId.newBuilder().setCreditCode(CreditCode).setCertName(certName).build())
                .setCertHash(DigestUtils.sha256Hex(certPem))
                .build();

        Peer.Signer signer = Peer.Signer.newBuilder()
                .setName(CreditCode)
                .setCreditCode(CreditCode)
                .setMobile("18888888888")
                .addAllCertNames(Arrays.asList(certName))
                .addAllAuthorizeIds(Collections.singletonList("auth-1"))
                .addAllOperateIds(Arrays.asList("operate-1"))
                .addAllCredentialMetadataIds(Arrays.asList("credential-metadata-1"))
                .addAllAuthenticationCerts(Arrays.asList(cert_proto))
                .setSignerValid(true)
                .build();
        return new SignerCert(signer, cert_proto);
    }

    /**
     *
     * @param creditCode
     * @param certName
     * @return
     */
    TranCreator genUsrTranCreator(String creditCode, String certName) {
        try {
            TranCreator tranCreator = TranCreator.newBuilder()
                    .setSignAlgorithm("SHA256withECDSA")
                    .setPrivateKey(KeyUtil.generatePrivateKey(new PEMParser(new FileReader(String.format("jks/did/%s_%s.pkcs8", creditCode, certName))), "123"))
                    .build();
            return tranCreator;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (OperatorCreationException e) {
            e.printStackTrace();
        } catch (PKCSException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *
     * @param creditCode
     * @param certName
     * @return
     */
    TranCreator genNodeTranCreator(String creditCode, String certName) {
        try {
            TranCreator tranCreator = TranCreator.newBuilder()
                    .setSignAlgorithm("SHA256withECDSA")
                    .setPrivateKey(KeyUtil.generatePrivateKey(new PEMParser(new FileReader(String.format("jks/jdk13/%s.%s.pkcs8", creditCode, certName))), "123"))
                    .build();
            return tranCreator;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (OperatorCreationException e) {
            e.printStackTrace();
        } catch (PKCSException e) {
            e.printStackTrace();
        }
        return null;
    }

    class SignerCert {
        Peer.Signer signer;
        Peer.Certificate certificate;

        public SignerCert(Peer.Signer signer, Peer.Certificate certificate) {
            this.signer = signer;
            this.certificate = certificate;
        }

        public Peer.Signer getSigner() {
            return signer;
        }

        public void setSigner(Peer.Signer signer) {
            this.signer = signer;
        }

        public Peer.Certificate getCertificate() {
            return certificate;
        }

        public void setCertificate(Peer.Certificate certificate) {
            this.certificate = certificate;
        }
    }


}
