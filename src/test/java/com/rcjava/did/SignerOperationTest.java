package com.rcjava.did;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.rcjava.model.Transfer;
import com.rcjava.protos.Peer;
import com.rcjava.tran.TranCreator;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.*;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author zyf
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SignerOperationTest extends DidTest {

    public SignerOperationTest() throws Exception {
    }

    @Test
    @Order(1)
    @DisplayName("测试注册账户-creditCode项为空")
    void testSignUpSignerCreditCodeBlank() throws Exception {
        String tranId = UUID.randomUUID().toString();
        Peer.Signer signer_1 = usr0_signer.toBuilder().clearCreditCode().build();
        Peer.Transaction tran = superCreator.createInvokeTran(tranId, superCertId, chaincodeId, signUpSigner, JsonFormat.printer().print(signer_1), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(5);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(102, actionResult.getCode(), "错误码为102");
        JSONObject errMsg = JSONObject.parseObject(actionResult.getReason());
        Assertions.assertEquals(12001, errMsg.getInteger("code"), "creditCode为空字符串");
    }

    @Test
    @Order(2)
    @DisplayName("测试注册账户-certNames项/authorizeIds不为空")
    void testSignUpSignerSomeFieldsBlank() throws Exception {
        String tranId_1 = UUID.randomUUID().toString();
        Peer.Signer signer_1 = usr0_signer.toBuilder().clearCertNames().build();
        Peer.Transaction tran_1 = superCreator.createInvokeTran(tranId_1, superCertId, chaincodeId, signUpSigner, JsonFormat.printer().print(signer_1), 0, "");
        postClient.postSignedTran(tran_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_1 = getTransactionResult(tranId_1);
        Peer.ActionResult actionResult_1 = tranResult_1.getErr();
        Assertions.assertEquals(102, actionResult_1.getCode(), "错误码为102");
        JSONObject errMsg_1 = JSONObject.parseObject(actionResult_1.getReason());
        Assertions.assertEquals(12004, errMsg_1.getInteger("code"), "certNames/authorizeIds/operateIds/credentialMetadataIds存在非空");

        String tranId_2 = UUID.randomUUID().toString();
        Peer.Signer signer_2 = usr0_signer.toBuilder().clearCertNames().clearAuthorizeIds().build();
        Peer.Transaction tran_2 = superCreator.createInvokeTran(tranId_2, superCertId, chaincodeId, signUpSigner, JsonFormat.printer().print(signer_2), 0, "");
        postClient.postSignedTran(tran_2);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_2 = getTransactionResult(tranId_2);
        Peer.ActionResult actionResult_2 = tranResult_2.getErr();
        Assertions.assertEquals(102, actionResult_2.getCode(), "错误码为102");
        JSONObject errMsg_2 = JSONObject.parseObject(actionResult_2.getReason());
        Assertions.assertEquals(12004, errMsg_2.getInteger("code"), "certNames/authorizeIds/operateIds/credentialMetadataIds存在非空");

    }


    @Test
    @Order(3)
    @DisplayName("测试注册账户-AuthCerts为空")
    void testSignUpSignerAuthCertEmpty() throws Exception {
        String tranId_1 = UUID.randomUUID().toString();
        Peer.Signer signer_1 = usr0_signer.toBuilder().clearCertNames().clearAuthorizeIds().clearOperateIds().clearCredentialMetadataIds().clearAuthenticationCerts().build();
        Peer.Transaction tran_1 = superCreator.createInvokeTran(tranId_1, superCertId, chaincodeId, signUpSigner, JsonFormat.printer().print(signer_1), 0, "");
        postClient.postSignedTran(tran_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_1 = getTransactionResult(tranId_1);
        Peer.ActionResult actionResult_1 = tranResult_1.getErr();
        Assertions.assertEquals(102, actionResult_1.getCode(), "错误码为102");
        JSONObject errMsg_1 = JSONObject.parseObject(actionResult_1.getReason());
        Assertions.assertEquals(12006, errMsg_1.getInteger("code"), "Signer未包含任何身份校验证书");

    }

    @Test
    @Order(4)
    @DisplayName("测试注册账户-存在Hash为空的身份证书")
    void testSignUpSignerAuthCertHashEmpty() throws Exception {
        Peer.Certificate usr_1_cer = Peer.Certificate.newBuilder()
                .setCertificate(user1_pem_0)
                .setAlgType("sha256withecdsa")
                .setCertValid(true)
                .setCertType(Peer.Certificate.CertType.CERT_AUTHENTICATION)
                .setId(Peer.CertId.newBuilder().setCreditCode(user1_creditCode).setCertName("1").build())
                .build();
        String tranId_1 = UUID.randomUUID().toString();
        Peer.Signer signer_1 = usr0_signer.toBuilder().clearCertNames().clearAuthorizeIds().clearOperateIds().clearCredentialMetadataIds()
                .addAuthenticationCerts(usr_1_cer).build();
        Peer.Transaction tran_1 = superCreator.createInvokeTran(tranId_1, superCertId, chaincodeId, signUpSigner, JsonFormat.printer().print(signer_1), 0, "");
        postClient.postSignedTran(tran_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_1 = getTransactionResult(tranId_1);
        Peer.ActionResult actionResult_1 = tranResult_1.getErr();
        Assertions.assertEquals(102, actionResult_1.getCode(), "错误码为102");
        JSONObject errMsg_1 = JSONObject.parseObject(actionResult_1.getReason());
        Assertions.assertEquals(12007, errMsg_1.getInteger("code"), "存在Hash为空的身份校验证书");

    }


    @Test
    @Order(5)
    @DisplayName("测试注册账户-存在非身份证书")
    void testSignUpSignerCustomCertExists() throws Exception {
        Peer.Certificate usr_1_cer = Peer.Certificate.newBuilder()
                .setCertificate(user1_pem_0)
                .setAlgType("sha256withecdsa")
                .setCertValid(true)
                .setCertType(Peer.Certificate.CertType.CERT_CUSTOM)
                .setId(Peer.CertId.newBuilder().setCreditCode(user0_creditCode).setCertName("1").build())
                .setCertHash(DigestUtils.sha256Hex(user1_pem_0))
                .build();
        String tranId_1 = UUID.randomUUID().toString();
        Peer.Signer signer_1 = usr0_signer.toBuilder().clearCertNames().clearAuthorizeIds().clearOperateIds().clearCredentialMetadataIds()
                .addAuthenticationCerts(usr_1_cer).build();
        Peer.Transaction tran_1 = superCreator.createInvokeTran(tranId_1, superCertId, chaincodeId, signUpSigner, JsonFormat.printer().print(signer_1), 0, "");
        postClient.postSignedTran(tran_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_1 = getTransactionResult(tranId_1);
        Peer.ActionResult actionResult_1 = tranResult_1.getErr();
        Assertions.assertEquals(102, actionResult_1.getCode(), "错误码为102");
        JSONObject errMsg_1 = JSONObject.parseObject(actionResult_1.getReason());
        Assertions.assertEquals(12008, errMsg_1.getInteger("code"), "存在普通用户证书或undefinedType证书");

    }

    @Test
    @Order(6)
    @DisplayName("测试注册账户-身份证书的creditCode与signer的creditCode不一致")
    void testSignUpSignerCreditCodeNotMatch() throws Exception {
        Peer.Certificate usr_1_cer = Peer.Certificate.newBuilder()
                .setCertificate(user1_pem_0)
                .setAlgType("sha256withecdsa")
                .setCertValid(true)
                .setCertType(Peer.Certificate.CertType.CERT_AUTHENTICATION)
                .setId(Peer.CertId.newBuilder().setCreditCode(user1_creditCode).setCertName("1").build())
                .setCertHash(DigestUtils.sha256Hex(user1_pem_0))
                .build();
        String tranId_1 = UUID.randomUUID().toString();
        Peer.Signer signer_1 = usr0_signer.toBuilder().clearCertNames().clearAuthorizeIds().clearOperateIds().clearCredentialMetadataIds()
                .addAuthenticationCerts(usr_1_cer).build();
        Peer.Transaction tran_1 = superCreator.createInvokeTran(tranId_1, superCertId, chaincodeId, signUpSigner, JsonFormat.printer().print(signer_1), 0, "");
        postClient.postSignedTran(tran_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_1 = getTransactionResult(tranId_1);
        Peer.ActionResult actionResult_1 = tranResult_1.getErr();
        Assertions.assertEquals(102, actionResult_1.getCode(), "错误码为102");
        JSONObject errMsg_1 = JSONObject.parseObject(actionResult_1.getReason());
        Assertions.assertEquals(12009, errMsg_1.getInteger("code"), "Signer的creditCode与Certificate中的creditCode不一致");

    }

    @Test
    @Order(7)
    @DisplayName("测试注册账户-Certificate中hash字段与certificate字段计算得到的Hash不相等")
    void testSignUpSignerHashNotMatch() throws Exception {
        Peer.Certificate usr_1_cer = Peer.Certificate.newBuilder()
                .setCertificate(user1_pem_0)
                .setAlgType("sha256withecdsa")
                .setCertValid(true)
                .setCertType(Peer.Certificate.CertType.CERT_AUTHENTICATION)
                .setId(Peer.CertId.newBuilder().setCreditCode(user0_creditCode).setCertName("1").build())
                .setCertHash(DigestUtils.sha256Hex(user1_pem_0) + "test")
                .build();
        String tranId_1 = UUID.randomUUID().toString();
        Peer.Signer signer_1 = usr0_signer.toBuilder().clearCertNames().clearAuthorizeIds().clearOperateIds().clearCredentialMetadataIds()
                .addAuthenticationCerts(usr_1_cer).build();
        Peer.Transaction tran_1 = superCreator.createInvokeTran(tranId_1, superCertId, chaincodeId, signUpSigner, JsonFormat.printer().print(signer_1), 0, "");
        postClient.postSignedTran(tran_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_1 = getTransactionResult(tranId_1);
        Peer.ActionResult actionResult_1 = tranResult_1.getErr();
        Assertions.assertEquals(102, actionResult_1.getCode(), "错误码为102");
        JSONObject errMsg_1 = JSONObject.parseObject(actionResult_1.getReason());
        Assertions.assertEquals(12010, errMsg_1.getInteger("code"), "Certificate中hash字段与certificate字段计算得到的Hash不相等");

    }

    @Test
    @Order(8)
    @DisplayName("测试注册账户-成功")
    void testSignUpSigner() throws Exception {
        String tranId_1 = UUID.randomUUID().toString();
        Peer.Signer signer_1 = usr0_signer.toBuilder().clearCertNames().clearAuthorizeIds().clearOperateIds().clearCredentialMetadataIds().build();
        Peer.Transaction tran_1 = superCreator.createInvokeTran(tranId_1, superCertId, chaincodeId, signUpSigner, JsonFormat.printer().print(signer_1), 0, "");
        postClient.postSignedTran(tran_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_1 = getTransactionResult(tranId_1);
        Assertions.assertEquals(0, tranResult_1.getErr().getCode(), "没有错误，注册成功");

    }

    @Test
    @Order(9)
    @DisplayName("测试注册账户-账户已存在")
    void testSignUpSignerExists() throws InvalidProtocolBufferException, InterruptedException {
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = superCreator.createInvokeTran(tranId, superCertId, chaincodeId, signUpSigner, JsonFormat.printer().print(usr0_signer), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(102, actionResult.getCode(), "错误码为102");
        JSONObject errMsg = JSONObject.parseObject(actionResult.getReason());
        Assertions.assertEquals(12002, errMsg.getInteger("code"), "账户已存在");
    }

    @Test
    @Order(10)
    @DisplayName("注册账户证书后, 可以提交交易到区块链")
    void testPostSignedTran() throws InterruptedException {
        String tranId = UUID.randomUUID().toString();
        TranCreator tranCreator = genUsrTranCreator(user0_creditCode, "0");
        Transfer transfer = new Transfer("usr_0", "12110107bi45jh675g", 5);
        Peer.Transaction tran = tranCreator.createInvokeTran(tranId, usr0_signer_cert.getCertificate().getId(), contractAssetsId, "transfer", JSON.toJSONString(transfer), 0, "");
        String tranHex = Hex.encodeHexString(tran.toByteArray());
        postClient.postSignedTran(tranHex);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(102, actionResult.getCode(), "错误码为102");
        Assertions.assertEquals("只允许从本人账户转出", actionResult.getReason());
    }

    @Test
    @Order(11)
    @DisplayName("测试注册账户-证书已存在, 账户2包含了账户1的证书，删除之后，即可注册usr1的账户成功")
    void testSignUpAuthCertExists() throws InvalidProtocolBufferException, InterruptedException {
        Peer.Certificate usr0_cert = usr0_signer_cert.getCertificate();
        String tranId = UUID.randomUUID().toString();
        Peer.Signer signer_1 = usr1_signer.toBuilder().clearCertNames().clearAuthorizeIds().clearOperateIds().clearCredentialMetadataIds()
                .addAuthenticationCerts(usr0_cert).build();
        Peer.Transaction tran = superCreator.createInvokeTran(tranId, superCertId, chaincodeId, signUpSigner, JsonFormat.printer().print(signer_1), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(102, actionResult.getCode(), "错误码为102");
        JSONObject errMsg = JSONObject.parseObject(actionResult.getReason());
        Assertions.assertEquals(12005, errMsg.getInteger("code"), String.format("Signer中的身份证书%s已存在", usr0_cert.getCertHash()));

        String tranId_1 = UUID.randomUUID().toString();
        signer_1 = usr1_signer.toBuilder().clearCertNames().clearAuthorizeIds().clearOperateIds().clearCredentialMetadataIds().build();
        Peer.Transaction tran_1 = superCreator.createInvokeTran(tranId_1, superCertId, chaincodeId, signUpSigner, JsonFormat.printer().print(signer_1), 0, "");
        postClient.postSignedTran(tran_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_1 = getTransactionResult(tranId_1);
        Assertions.assertEquals(0, tranResult_1.getErr().getCode(), "没有错误，注册成功");
    }

    @Test
    @Order(12)
    @DisplayName("修改账户状态-非管理员不能修改管理员的账户状态")
    void testUpdateSignerStatus_0() throws InterruptedException {
        String tranId = UUID.randomUUID().toString();
        JSONObject status = new JSONObject();
        status.fluentPut("creditCode", super_creditCode);
        status.fluentPut("state", false);
        Peer.Transaction tran = node1Creator.createInvokeTran(tranId, node1CertId, chaincodeId, updateSignerStatus, status.toJSONString(), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(102, actionResult.getCode(), "错误码为102");
        JSONObject errMsg = JSONObject.parseObject(actionResult.getReason());
        Assertions.assertEquals(12011, errMsg.getInteger("code"), "非super_admin不能修改super_admin的signer的状态");
    }

    @Test
    @Order(13)
    @DisplayName("修改账户状态-账户不存在")
    void testUpdateSignerStatus_1() throws InterruptedException {
        String tranId = UUID.randomUUID().toString();
        JSONObject status = new JSONObject();
        status.fluentPut("creditCode", "not-exists");
        status.fluentPut("state", false);
        Peer.Transaction tran = superCreator.createInvokeTran(tranId, superCertId, chaincodeId, updateSignerStatus, status.toJSONString(), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(102, actionResult.getCode(), "错误码为102");
        JSONObject errMsg = JSONObject.parseObject(actionResult.getReason());
        Assertions.assertEquals(12003, errMsg.getInteger("code"), "Signer账户实体不存在");
    }

    @RepeatedTest(name = "重复修改状态", value =  2)
    @Order(14)
    @DisplayName("修改账户状态-管理员修改node1的账户状态")
    void testUpdateSignerStatus_2() throws InterruptedException {
        String tranId = UUID.randomUUID().toString();
        JSONObject status = new JSONObject();
        status.fluentPut("creditCode", node1_creditCode);
        status.fluentPut("state", false);
        Peer.Transaction tran = superCreator.createInvokeTran(tranId, superCertId, chaincodeId, updateSignerStatus, status.toJSONString(), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Assertions.assertEquals(0, tranResult.getErr().getCode(), "没有错误，修改成功");

        String tranId_1 = UUID.randomUUID().toString();
        Transfer transfer = new Transfer(node1_creditCode, "12110107bi45jh675g", 5);
        Peer.Transaction tran_1 = node1Creator.createInvokeTran(tranId_1, node1CertId, contractAssetsId, "transfer", JSON.toJSONString(transfer), 0, "");
        String tranHex_1 = Hex.encodeHexString(tran_1.toByteArray());
        postClient.postSignedTran(tranHex_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_1 = getTransactionResult(tranId_1);
        Peer.ActionResult actionResult = tranResult_1.getErr();
        Assertions.assertEquals(101, actionResult.getCode(), "错误码为101");
        Assertions.assertEquals("实体账户已经失效", actionResult.getReason());

        String tranId_2 = UUID.randomUUID().toString();
        status.fluentPut("state", true);
        Peer.Transaction tran_2 = superCreator.createInvokeTran(tranId_2, superCertId, chaincodeId, updateSignerStatus, status.toJSONString(), 0, "");
        postClient.postSignedTran(tran_2);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_2 = infoClient.getBlockByHeight(infoClient.getChainInfo().getHeight()).getTransactionResults(0);
        Assertions.assertEquals(tranId_2, tranResult_2.getTxId());
        Assertions.assertEquals(0, tranResult_2.getErr().getCode(), "没有错误，修改成功");
    }

}
