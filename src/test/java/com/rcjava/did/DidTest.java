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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author zyf
 */
public class DidTest {

    Logger logger = LoggerFactory.getLogger(getClass());

    String host = "localhost:9081";
    protected TranPostClient postClient = new TranPostClient(host);
    protected ChainInfoClient infoClient = new ChainInfoClient(host);

    protected Peer.ChaincodeId contractAssetsId = Peer.ChaincodeId.newBuilder().setChaincodeName("ContractAssetsTPL").setVersion(1).build();
    protected Peer.ChaincodeId didChaincodeId = Peer.ChaincodeId.newBuilder().setChaincodeName("RdidOperateAuthorizeTPL").setVersion(1).build();

    protected String did_network_id = "identity-net:";

    protected String node1_creditCode = "identity-net:121000005l35120456";
    protected String node2_creditCode = "identity-net:12110107bi45jh675g";
    protected String node3_creditCode = "identity-net:122000002n00123567";
    protected String node4_creditCode = "identity-net:921000005k36123789";
    protected String node5_creditCode = "identity-net:921000006e0012v696";
    protected String super_creditCode = "identity-net:951002007l78123233";

    protected static String signUpSigner = "signUpSigner";
    protected static String updateSigner = "updateSigner";
    protected static String updateSignerStatus = "updateSignerStatus";

    protected static String signUpCertificate = "signUpCertificate"; // 无需授权
    protected static String updateCertificateStatus = "updateCertificateStatus"; // 无需授权
    protected static String signUpAllTypeCertificate = "signUpAllTypeCertificate"; // 需授权
    protected static String updateAllTypeCertificateStatus = "updateAllTypeCertificateStatus"; // 需授权

    protected static String grantOperate = "grantOperate";
    protected static String updateGrantOperateStatus = "updateGrantOperateStatus";
    protected static String bindCertToAuthorize = "bindCertToAuthorize";

    protected static String signUpOperate = "signUpOperate";
    protected static String updateOperateStatus = "updateOperateStatus";

    protected PrivateKey super_pri = null;
    protected Peer.CertId superCertId = null;
    protected TranCreator superCreator = null;

    protected PrivateKey node1_pri = null;
    protected Peer.CertId node1CertId = null;
    protected TranCreator node1Creator = null;

    protected static String user0_creditCode_did = "identity-net:usr-0";
    protected static String user1_creditCode_did = "identity-net:usr-1";
    protected static String user2_creditCode_did = "identity-net:usr-2";

    protected static String user0_creditCode_cre = "credence-net:usr-0";
    protected static String user1_creditCode_cre = "credence-net:usr-1";
    protected static String user2_creditCode_cre = "credence-net:usr-2";

    // 身份证书
    static String user0_cert_0 = "0";
    // 普通证书
    static String user0_cert_1 = "1";
    // 身份证书
    static String user1_cert_0 = "0";
    // 普通证书
    static String user1_cert_1 = "1";
    // 身份证书
    static String user2_cert_0 = "0";
    // 普通证书
    static String user2_cert_1 = "1";

    Peer.CertId usr0_certId_0 = Peer.CertId.newBuilder().setCreditCode(user0_creditCode_did).setCertName(user0_cert_0).build();
    Peer.CertId usr0_certId_1 = Peer.CertId.newBuilder().setCreditCode(user0_creditCode_did).setCertName(user0_cert_1).build();

    Peer.CertId usr1_certId_0 = Peer.CertId.newBuilder().setCreditCode(user1_creditCode_did).setCertName(user1_cert_0).build();
    Peer.CertId usr1_certId_1 = Peer.CertId.newBuilder().setCreditCode(user1_creditCode_did).setCertName(user1_cert_1).build();

    Peer.CertId usr2_certId_0 = Peer.CertId.newBuilder().setCreditCode(user2_creditCode_did).setCertName(user2_cert_0).build();
    Peer.CertId usr2_certId_1 = Peer.CertId.newBuilder().setCreditCode(user2_creditCode_did).setCertName(user2_cert_1).build();

    TranCreator usr0_tranCreator_0 = genUsrTranCreator(user0_creditCode_did, user0_cert_0);
    TranCreator usr0_tranCreator_1 = genUsrTranCreator(user0_creditCode_did, user0_cert_1);

    TranCreator usr1_tranCreator_0 = genUsrTranCreator(user1_creditCode_did, user1_cert_0);
    TranCreator usr1_tranCreator_1 = genUsrTranCreator(user1_creditCode_did, user1_cert_1);

    TranCreator usr2_tranCreator_0 = genUsrTranCreator(user2_creditCode_did, user2_cert_0);
    TranCreator usr2_tranCreator_1 = genUsrTranCreator(user2_creditCode_did, user2_cert_1);


//    DidTest.SignerCert usr0_signer_cert_1 = genCertSigner(user0_creditCode, user0_pem_1, user0_cert_1);
//    DidTest.SignerCert usr1_signer_cert_1 = genCertSigner(user1_creditCode, user1_pem_1, user1_cert_1);

    String user0_pem_0 = new String(Files.readAllBytes(new File(String.format("jks/did/%s_%s.cer", user0_creditCode_did.substring(did_network_id.length()), user0_cert_0)).toPath()));
    String user1_pem_0 = new String(Files.readAllBytes(new File(String.format("jks/did/%s_%s.cer", user1_creditCode_did.substring(did_network_id.length()), user1_cert_0)).toPath()));
    String user2_pem_0 = new String(Files.readAllBytes(new File(String.format("jks/did/%s_%s.cer", user2_creditCode_did.substring(did_network_id.length()), user2_cert_0)).toPath()));

    SignerCert usr0_signer_cert = getCertSigner(user0_creditCode_did, user0_pem_0, user0_cert_0);
    SignerCert usr1_signer_cert = getCertSigner(user1_creditCode_did, user1_pem_0, user1_cert_0);
    SignerCert usr2_signer_cert = getCertSigner(user2_creditCode_did, user2_pem_0, user2_cert_0);

    Peer.Signer usr0_signer = usr0_signer_cert.getSigner();
    Peer.Signer usr1_signer = usr1_signer_cert.getSigner();
    Peer.Signer usr2_signer = usr2_signer_cert.getSigner();

    @Test
    @BeforeAll
    void init() {
        super_pri = CertUtil.genX509CertPrivateKey(new File("jks/jdk13/951002007l78123233.super_admin.jks"),
                "super_admin", "951002007l78123233.super_admin").getPrivateKey();
        superCreator = TranCreator.newBuilder().setPrivateKey(super_pri).setSignAlgorithm("SHA256withECDSA").build();
        superCertId = Peer.CertId.newBuilder().setCreditCode(super_creditCode).setCertName("super_admin").build();
        node1_pri = CertUtil.genX509CertPrivateKey(new File("jks/jdk13/121000005l35120456.node1.jks"),
                "123", "121000005l35120456.node1").getPrivateKey();
        node1CertId = Peer.CertId.newBuilder().setCreditCode(node1_creditCode).setCertName("node1").build();
        node1Creator = TranCreator.newBuilder().setPrivateKey(node1_pri).setSignAlgorithm("SHA256withECDSA").build();
    }

    public DidTest() throws IOException {
    }

    public static void main(String[] args) throws Exception {
//        genCertPriKeyJks();
    }

    /**
     * 生成证书和私钥
     *
     * @throws Exception
     */
    static void genCertPriKeyJks() throws Exception {
        for (int i = 0; i <= 3; i++) {
            for (int j = 0; j < 2; j++) {
                String usr_alias = "usr-" + i + "_" + j;
                CertUtil.genJksFile(new File("jks/did/" + usr_alias + ".jks"), usr_alias, "123");
            }
        }
        for (int i = 0; i <= 3; i++) {
            for (int j = 0; j < 2; j++) {
                String usr_alias = "usr-" + i + "_" + j;
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
    protected SignerCert getCertSigner(String CreditCode, String certPem, String certName) {
        Peer.Certificate cert_proto = Peer.Certificate.newBuilder()
                .setCertificate(certPem)
                .setAlgType("sha256withecdsa")
                .setCertValid(true)
                .setCertType(Peer.Certificate.CertType.CERT_AUTHENTICATION)
                .setId(Peer.CertId.newBuilder().setCreditCode(CreditCode).setCertName(certName).build())
                .setCertHash(DigestUtils.sha256Hex((did_network_id + certPem).replaceAll("\r\n|\r|\n|\\s", "")))
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
     * @param creditCode
     * @param certName
     * @return
     * @throws IOException
     */
    protected Peer.Certificate getNodeCert(String creditCode, String certName) throws IOException {
        X509Certificate x509Certificate = CertUtil.genX509CertPrivateKey(
                new File(String.format("jks/jdk13/%s.%s.jks", creditCode.substring(did_network_id.length()), certName)),
                "123",
                creditCode + "." + certName).getCertificate();
        String certPem = PemUtil.toPemString(x509Certificate);
        return Peer.Certificate.newBuilder()
                .setCertificate(certPem)
                .setAlgType("sha256withecdsa")
                .setCertValid(true)
                .setCertType(Peer.Certificate.CertType.CERT_CUSTOM)
                .setId(Peer.CertId.newBuilder().setCreditCode(creditCode).setCertName(certName).build())
                .setCertHash(DigestUtils.sha256Hex(did_network_id + certPem.replaceAll("\r\n|\r|\n|\\s", "")))
                .build();
    }

    /**
     * @param creditCode
     * @param certName
     * @return
     */
    protected Peer.Certificate getUsrCert(String creditCode, String certName) {
        X509Certificate x509Certificate = CertUtil.generateX509Cert(new File(String.format("jks/did/%s_%s.cer", creditCode.substring(did_network_id.length()), certName)));
        try {
            String certPem = PemUtil.toPemString(x509Certificate);
            return Peer.Certificate.newBuilder()
                    .setCertificate(certPem)
                    .setAlgType("sha256withecdsa")
                    .setCertValid(true)
                    .setCertType(Peer.Certificate.CertType.CERT_CUSTOM)
                    .setId(Peer.CertId.newBuilder().setCreditCode(creditCode).setCertName(certName).build())
                    .setCertHash(DigestUtils.sha256Hex(did_network_id + certPem.replaceAll("\r\n|\r|\n|\\s", "")))
                    .build();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param creditCode
     * @param certName
     * @return
     */
    protected TranCreator genUsrTranCreator(String creditCode, String certName) {
        try {
            PrivateKey privateKey = KeyUtil.generatePrivateKey(
                    new PEMParser(new FileReader(String.format("jks/did/%s_%s.pkcs8", creditCode.substring(did_network_id.length()), certName))),
                    "123");
            TranCreator tranCreator = TranCreator.newBuilder()
                    .setSignAlgorithm("SHA256withECDSA")
                    .setPrivateKey(privateKey)
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
     * @param creditCode
     * @param certName
     * @return
     */
    protected TranCreator genNodeTranCreator(String creditCode, String certName) {
        PrivateKey privateKey = CertUtil.genX509CertPrivateKey(
                new File(String.format("jks/jdk13/%s.%s.jks", creditCode.substring(did_network_id.length()), certName)),
                "123",
                creditCode + "." + certName).getPrivateKey();
        TranCreator tranCreator = TranCreator.newBuilder()
                .setSignAlgorithm("SHA256withECDSA")
                .setPrivateKey(privateKey)
                .build();
        return tranCreator;
    }

    /**
     * 根据交易ID获取对应的交易结果
     *
     * @param txid 交易id
     * @return
     */
    protected Peer.TransactionResult getTransactionResult(String txid) {
        ChainInfoClient.TranInfoAndHeight infoAndHeight = infoClient.getTranInfoAndHeightByTranId(txid);
        if (infoAndHeight == null) {
            return null;
        }
        logger.info("\ntxid:{}, \nheight:{}", txid, infoAndHeight.getHeight());
        Peer.Block block = infoClient.getBlockByHeight(infoAndHeight.getHeight());
        return block.getTransactionResultsList().stream()
                .filter(tranResult -> tranResult.getTxId().equals(txid))
                .findAny()
                .orElse(null);
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

    public Peer.Signer getUsr0_signer() {
        return usr0_signer;
    }

    public Peer.Signer getUsr1_signer() {
        return usr1_signer;
    }

    public Peer.Signer getUsr2_signer() {
        return usr2_signer;
    }

    public Peer.CertId getUsr0_certId_0() {
        return usr0_certId_0;
    }

    public Peer.CertId getUsr1_certId_0() {
        return usr1_certId_0;
    }

    public Peer.CertId getUsr2_certId_0() {
        return usr2_certId_0;
    }

    public TranCreator getUsr0_tranCreator_0() {
        return usr0_tranCreator_0;
    }

    public TranCreator getUsr1_tranCreator_0() {
        return usr1_tranCreator_0;
    }

    public TranCreator getUsr2_tranCreator_0() {
        return usr2_tranCreator_0;
    }
}
