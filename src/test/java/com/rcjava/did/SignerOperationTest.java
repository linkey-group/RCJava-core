package com.rcjava.did;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.rcjava.model.Transfer;
import com.rcjava.protos.Peer;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.*;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author zyf
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SignerOperationTest extends DidTest {

    String user0_pem_0 = new String(Files.readAllBytes(new File(String.format("jks/test/did/%s_%s.cer", user0_creditCode_did.split(":")[1], user0_cert_0)).toPath()));
    String user1_pem_0 = new String(Files.readAllBytes(new File(String.format("jks/test/did/%s_%s.cer", user1_creditCode_did.split(":")[1], user1_cert_0)).toPath()));
    String user2_pem_0 = new String(Files.readAllBytes(new File(String.format("jks/test/did/%s_%s.cer", user2_creditCode_did.split(":")[1], user2_cert_0)).toPath()));

    public SignerOperationTest() throws Exception {
    }

    @Test
    @Order(1)
    @DisplayName("测试注册账户-creditCode项为空")
    void testSignUpSignerCreditCodeBlank() throws Exception {
        String tranId = UUID.randomUUID().toString();
        Peer.Signer signer_1 = usr0_signer.toBuilder().clearCreditCode().build();
        Peer.Transaction tran = superCreator.createInvokeTran(tranId, superCertId, didChaincodeId, signUpSigner, JsonFormat.printer().print(signer_1), 0, "");
        postClient.postSignedTran(tran);
        Peer.ActionResult actionResult = checkResult(tranId);
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
        Peer.Transaction tran_1 = superCreator.createInvokeTran(tranId_1, superCertId, didChaincodeId, signUpSigner, JsonFormat.printer().print(signer_1), 0, "");
        postClient.postSignedTran(tran_1);
        Peer.ActionResult actionResult_1 = checkResult(tranId_1);
        Assertions.assertEquals(102, actionResult_1.getCode(), "错误码为102");
        JSONObject errMsg_1 = JSONObject.parseObject(actionResult_1.getReason());
        Assertions.assertEquals(12004, errMsg_1.getInteger("code"), "certNames/authorizeIds/operateIds/credentialMetadataIds存在非空");

        String tranId_2 = UUID.randomUUID().toString();
        Peer.Signer signer_2 = usr0_signer.toBuilder().clearCertNames().clearAuthorizeIds().build();
        Peer.Transaction tran_2 = superCreator.createInvokeTran(tranId_2, superCertId, didChaincodeId, signUpSigner, JsonFormat.printer().print(signer_2), 0, "");
        postClient.postSignedTran(tran_2);
        Peer.ActionResult actionResult_2 = checkResult(tranId_2);
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
        Peer.Transaction tran_1 = superCreator.createInvokeTran(tranId_1, superCertId, didChaincodeId, signUpSigner, JsonFormat.printer().print(signer_1), 0, "");
        postClient.postSignedTran(tran_1);
        Peer.ActionResult actionResult_1 = checkResult(tranId_1);
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
                .setId(Peer.CertId.newBuilder().setCreditCode(user1_creditCode_did).setCertName("1").build())
                .build();
        String tranId_1 = UUID.randomUUID().toString();
        Peer.Signer signer_1 = usr0_signer.toBuilder().clearCertNames().clearAuthorizeIds().clearOperateIds().clearCredentialMetadataIds()
                .addAuthenticationCerts(usr_1_cer).build();
        Peer.Transaction tran_1 = superCreator.createInvokeTran(tranId_1, superCertId, didChaincodeId, signUpSigner, JsonFormat.printer().print(signer_1), 0, "");
        postClient.postSignedTran(tran_1);
        Peer.ActionResult actionResult_1 = checkResult(tranId_1);
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
                .setId(Peer.CertId.newBuilder().setCreditCode(user0_creditCode_did).setCertName("1").build())
                .setCertHash(DigestUtils.sha256Hex(user1_pem_0.replaceAll("\r\n|\r|\n|\\s", "")))
                .build();
        String tranId_1 = UUID.randomUUID().toString();
        Peer.Signer signer_1 = usr0_signer.toBuilder().clearCertNames().clearAuthorizeIds().clearOperateIds().clearCredentialMetadataIds()
                .addAuthenticationCerts(usr_1_cer).build();
        Peer.Transaction tran_1 = superCreator.createInvokeTran(tranId_1, superCertId, didChaincodeId, signUpSigner, JsonFormat.printer().print(signer_1), 0, "");
        postClient.postSignedTran(tran_1);
        Peer.ActionResult actionResult_1 = checkResult(tranId_1);
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
                .setId(Peer.CertId.newBuilder().setCreditCode(user1_creditCode_did).setCertName("1").build())
                .setCertHash(DigestUtils.sha256Hex(user1_pem_0.replaceAll("\r\n|\r|\n|\\s", "")))
                .build();
        String tranId_1 = UUID.randomUUID().toString();
        Peer.Signer signer_1 = usr0_signer.toBuilder().clearCertNames().clearAuthorizeIds().clearOperateIds().clearCredentialMetadataIds()
                .addAuthenticationCerts(usr_1_cer).build();
        Peer.Transaction tran_1 = superCreator.createInvokeTran(tranId_1, superCertId, didChaincodeId, signUpSigner, JsonFormat.printer().print(signer_1), 0, "");
        postClient.postSignedTran(tran_1);
        Peer.ActionResult actionResult_1 = checkResult(tranId_1);
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
                .setId(Peer.CertId.newBuilder().setCreditCode(user0_creditCode_did).setCertName("1").build())
                .setCertHash(DigestUtils.sha256Hex(user1_pem_0.replaceAll("\r\n|\r|\n|\\s", "")) + "test")
                .build();
        String tranId_1 = UUID.randomUUID().toString();
        Peer.Signer signer_1 = usr0_signer.toBuilder().clearCertNames().clearAuthorizeIds().clearOperateIds().clearCredentialMetadataIds()
                .addAuthenticationCerts(usr_1_cer).build();
        Peer.Transaction tran_1 = superCreator.createInvokeTran(tranId_1, superCertId, didChaincodeId, signUpSigner, JsonFormat.printer().print(signer_1), 0, "");
        postClient.postSignedTran(tran_1);
        Peer.ActionResult actionResult_1 = checkResult(tranId_1);
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
        Peer.Transaction tran_1 = superCreator.createInvokeTran(tranId_1, superCertId, didChaincodeId, signUpSigner, JsonFormat.printer().print(signer_1), 0, "");
        postClient.postSignedTran(tran_1);
        Peer.ActionResult actionResult_1 = checkResult(tranId_1);
        Assertions.assertEquals(0, actionResult_1.getCode(), "没有错误，注册成功");

    }

    @Test
    @Order(9)
    @DisplayName("测试注册账户-账户已存在")
    void testSignUpSignerExists() throws InvalidProtocolBufferException, InterruptedException {
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = superCreator.createInvokeTran(tranId, superCertId, didChaincodeId, signUpSigner, JsonFormat.printer().print(usr0_signer), 0, "");
        postClient.postSignedTran(tran);
        Peer.ActionResult actionResult = checkResult(tranId);
        Assertions.assertEquals(102, actionResult.getCode(), "错误码为102");
        JSONObject errMsg = JSONObject.parseObject(actionResult.getReason());
        Assertions.assertEquals(12002, errMsg.getInteger("code"), "账户已存在");
    }

    @Test
    @Order(10)
    @DisplayName("注册账户证书后, 可以提交交易到区块链")
    void testPostSignedTran() throws InterruptedException {
        String tranId = UUID.randomUUID().toString();
        Transfer transfer = new Transfer("usr_0", "12110107bi45jh675g", 5);
        Peer.Transaction tran = usr0_tranCreator_0.createInvokeTran(tranId, usr0_signer_cert.getCertificate().getId(), contractAssetsId, "transfer", JSON.toJSONString(transfer), 0, "");
        String tranHex = Hex.encodeHexString(tran.toByteArray());
        postClient.postSignedTran(tranHex);
        Peer.ActionResult actionResult = checkResult(tranId);
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
        Peer.Transaction tran = superCreator.createInvokeTran(tranId, superCertId, didChaincodeId, signUpSigner, JsonFormat.printer().print(signer_1), 0, "");
        postClient.postSignedTran(tran);
        Peer.ActionResult actionResult = checkResult(tranId);
        Assertions.assertEquals(102, actionResult.getCode(), "错误码为102");
        JSONObject errMsg = JSONObject.parseObject(actionResult.getReason());
        Assertions.assertEquals(12005, errMsg.getInteger("code"), String.format("Signer中的身份证书%s已存在", usr0_cert.getCertHash()));

        String tranId_1 = UUID.randomUUID().toString();
        signer_1 = usr1_signer.toBuilder().clearCertNames().clearAuthorizeIds().clearOperateIds().clearCredentialMetadataIds().build();
        Peer.Transaction tran_1 = superCreator.createInvokeTran(tranId_1, superCertId, didChaincodeId, signUpSigner, JsonFormat.printer().print(signer_1), 0, "");
        postClient.postSignedTran(tran_1);
        Peer.ActionResult actionResult_1 = checkResult(tranId_1);
        Assertions.assertEquals(0, actionResult_1.getCode(), "没有错误，注册成功");
    }

    @Test
    @Order(12)
    @DisplayName("修改账户状态-非管理员不能修改管理员的账户状态")
    void testUpdateSignerStatus_0() throws InterruptedException {
        String tranId = UUID.randomUUID().toString();
        JSONObject status = new JSONObject();
        status.fluentPut("creditCode", super_creditCode);
        status.fluentPut("state", false);
        Peer.Transaction tran = node1Creator.createInvokeTran(tranId, node1CertId, didChaincodeId, updateSignerStatus, status.toJSONString(), 0, "");
        postClient.postSignedTran(tran);
        Peer.ActionResult actionResult = checkResult(tranId);
        Assertions.assertEquals(102, actionResult.getCode(), "错误码为102");
        JSONObject errMsg = JSONObject.parseObject(actionResult.getReason());
        Assertions.assertEquals(12011, errMsg.getInteger("code"), "非super_admin不能修改super_admin的signer的信息或状态");
    }

    @Test
    @Order(13)
    @DisplayName("修改账户状态-账户不存在")
    void testUpdateSignerStatus_1() throws InterruptedException {
        String tranId = UUID.randomUUID().toString();
        JSONObject status = new JSONObject();
        status.fluentPut("creditCode", "not-exists");
        status.fluentPut("state", false);
        Peer.Transaction tran = superCreator.createInvokeTran(tranId, superCertId, didChaincodeId, updateSignerStatus, status.toJSONString(), 0, "");
        postClient.postSignedTran(tran);
        Peer.ActionResult actionResult = checkResult(tranId);
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
        Peer.Transaction tran = superCreator.createInvokeTran(tranId, superCertId, didChaincodeId, updateSignerStatus, status.toJSONString(), 0, "");
        postClient.postSignedTran(tran);
        Peer.ActionResult actionResult = checkResult(tranId);
        Assertions.assertEquals(0, actionResult.getCode(), "没有错误，修改成功");

        String tranId_1 = UUID.randomUUID().toString();
        Transfer transfer = new Transfer(node1_creditCode, node2_creditCode, 5);
        Peer.Transaction tran_1 = node1Creator.createInvokeTran(tranId_1, node1CertId, contractAssetsId, "transfer", JSON.toJSONString(transfer), 0, "");
        String tranHex_1 = Hex.encodeHexString(tran_1.toByteArray());
        postClient.postSignedTran(tranHex_1);
        actionResult = checkResult(tranId_1);
        Assertions.assertEquals(101, actionResult.getCode(), "错误码为101");
        Assertions.assertEquals("实体账户已经失效", actionResult.getReason());

        String tranId_2 = UUID.randomUUID().toString();
        status.fluentPut("state", true);
        Peer.Transaction tran_2 = superCreator.createInvokeTran(tranId_2, superCertId, didChaincodeId, updateSignerStatus, status.toJSONString(), 0, "");
        postClient.postSignedTran(tran_2);
        actionResult = checkResult(tranId_2);
        Assertions.assertEquals(0, actionResult.getCode(), "没有错误，修改成功");
    }

    @RepeatedTest(name = "重复修改账户", value =  2)
    @Order(15)
    @DisplayName("修改账户-有权限的可调用updateSigner修改账户")
    void testUpdateSigner_1() throws InterruptedException, InvalidProtocolBufferException {
        // step1: node1不能修改superAdmin的账户信息
        Peer.Signer signer_1 = Peer.Signer.newBuilder().setCreditCode(super_creditCode).setMobile("modify-Mobile").setSignerInfo("modify-signerInfo").build();
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = node1Creator.createInvokeTran(tranId, node1CertId, didChaincodeId, updateSigner, JsonFormat.printer().print(signer_1), 0, "");
        postClient.postSignedTran(tran);
        Peer.ActionResult actionResult = checkResult(tranId);
        Assertions.assertEquals(102, actionResult.getCode(), "错误码为102");
        JSONObject errMsg = JSONObject.parseObject(actionResult.getReason());
        Assertions.assertEquals(12011, errMsg.getInteger("code"), "非super_admin不能修改super_admin的signer的信息状态");

        // step2: node1为node2修改账户信息
        String tranId_2 = UUID.randomUUID().toString();
        Peer.Signer signer_2 = Peer.Signer.newBuilder().setCreditCode(node2_creditCode).setMobile("modify-Mobile").setSignerInfo("modify-signerInfo").build();
        Peer.Transaction tran_2 = node1Creator.createInvokeTran(tranId_2, node1CertId, didChaincodeId, updateSigner, JsonFormat.printer().print(signer_2), 0, "");
        postClient.postSignedTran(tran_2);
        Peer.ActionResult tranResult_2 = checkResult(tranId_2);
        Assertions.assertEquals(0, tranResult_2.getCode(), "没有错误，修改成功");

        // step3: 要修改的账户不存在
        String tranId_3 = UUID.randomUUID().toString();
        Peer.Signer signer_3 = Peer.Signer.newBuilder().setCreditCode("not-exists").setMobile("modify-Mobile").setSignerInfo("modify-signerInfo").build();
        Peer.Transaction tran_3 = node1Creator.createInvokeTran(tranId_3, node1CertId, didChaincodeId, updateSigner, JsonFormat.printer().print(signer_3), 0, "");
        postClient.postSignedTran(tran_3);
        Peer.ActionResult actionResult_3 = checkResult(tranId_3);
        Assertions.assertEquals(102, actionResult_3.getCode(), "错误码为102");
        JSONObject errMsg_3 = JSONObject.parseObject(actionResult_3.getReason());
        Assertions.assertEquals(12003, errMsg_3.getInteger("code"), "Signer账户实体不存在");
    }

}
