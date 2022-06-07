package com.rcjava.did;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import com.rcjava.model.Transfer;
import com.rcjava.protos.Peer;
import com.rcjava.tran.TranCreator;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;

/**
 * 本轮结束后
 * 1. usr0拥有的auth: signUpAllTypeCertificate
 * 2. usr1拥有的auth: signUpAllTypeCertificate, updateAllTypeCertificateStatus
 *
 * @author zyf
 */
@Tags({@Tag("注册普通证书"), @Tag("修改普通证书状态"), @Tag("注册任何证书"), @Tag("修改任何证书状态")})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CertOpeartionTest extends DidTest {

    public CertOpeartionTest() throws IOException {
    }

    @Tag("注册普通证书")
    @Test
    @Order(1)
    @DisplayName("测试注册证书-usr0使用signUpCertificate为别人注册证书, 失败，只能注册自己的证书")
    void testSignUpCert_1() throws IOException, InterruptedException {
        // usr0为自己注册普通证书(credtiCode为usr1的), 会失败
        Peer.Certificate usr1_cert_0 = getUsrCert(user1_creditCode, user1_cert_0);
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = usr0_tranCreator_0.createInvokeTran(tranId, usr0_certId_0, didChaincodeId, signUpCertificate, JsonFormat.printer().print(usr1_cert_0), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(102, actionResult.getCode(), "错误码为102");
        JSONObject errMsg = JSONObject.parseObject(actionResult.getReason());
        Assertions.assertEquals(13002, errMsg.getInteger("code"), "creditCode不匹配");
    }

    @Tag("注册普通证书")
    @Test
    @Order(2)
    @DisplayName("测试注册证书-usr0使用signUpCertificate为自己注册证书，但是证书已存在")
    void testSignUpCert_2() throws IOException, InterruptedException {
        Peer.Certificate usr0_cert = getUsrCert(user0_creditCode, user0_cert_0);
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = usr0_tranCreator_0.createInvokeTran(tranId, usr0_certId_0, didChaincodeId, signUpCertificate, JsonFormat.printer().print(usr0_cert), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(102, actionResult.getCode(), "错误码为102");
        JSONObject errMsg = JSONObject.parseObject(actionResult.getReason());
        Assertions.assertEquals(13004, errMsg.getInteger("code"), "证书已存在");
    }

    @Tag("注册普通证书")
    @Test
    @Order(3)
    @DisplayName("测试注册证书-usr0使用signUpCertificate为自己注册证书，但是证书Hash不匹配")
    void testSignUpCert_3() throws IOException, InterruptedException {
        // 构造一个不同的证书
        Peer.Certificate usr0_cert_1 = getUsrCert(user0_creditCode, user0_cert_1).toBuilder().setCertHash("test").build();
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = usr0_tranCreator_0.createInvokeTran(tranId, usr0_certId_0, didChaincodeId, signUpCertificate, JsonFormat.printer().print(usr0_cert_1), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(102, actionResult.getCode(), "错误码为102");
        JSONObject errMsg = JSONObject.parseObject(actionResult.getReason());
        Assertions.assertEquals(13008, errMsg.getInteger("code"), "Certificate中hash字段与certificate字段计算得到的Hash不相等");
    }

    @Tag("注册普通证书")
    @Test
    @Order(4)
    @DisplayName("测试注册证书-usr0使用signUpCertificate注册AuthCert")
    void testSignUpCert_4() throws IOException, InterruptedException {
        // 构造一个不同的证书
        Peer.Certificate usr0_cert_1 = getUsrCert(user0_creditCode, user0_cert_1).toBuilder()
                .setCertType(Peer.Certificate.CertType.CERT_AUTHENTICATION)
                .build();
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = usr0_tranCreator_0.createInvokeTran(tranId, usr0_certId_0, didChaincodeId, signUpCertificate, JsonFormat.printer().print(usr0_cert_1), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(102, actionResult.getCode(), "错误码为102");
        JSONObject errMsg = JSONObject.parseObject(actionResult.getReason());
        Assertions.assertEquals(13001, errMsg.getInteger("code"), "该方法不能被注册身份校验证书，请通过 signUpAllTypeCertificate");
        Assertions.assertEquals("该方法不能被注册身份校验证书，请通过 signUpAllTypeCertificate", errMsg.getString("reason"));
    }

    @Tag("注册普通证书")
    @Test
    @Order(5)
    @DisplayName("测试注册证书-usr0使用signUpCertificate注册CustomCert, 并提交交易")
    void testSignUpCert_5() throws IOException, InterruptedException {
        // step1: 注册证书
        Peer.Certificate usr0_cert_1 = getUsrCert(user0_creditCode, user0_cert_1);
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = usr0_tranCreator_0.createInvokeTran(tranId, usr0_certId_0, didChaincodeId, signUpCertificate, JsonFormat.printer().print(usr0_cert_1), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Assertions.assertEquals(0, tranResult.getErr().getCode(), "没有错误，注册成功");
        // step2: 使用注册好的证书对应的私钥提交签名交易
        String tranId_1 = UUID.randomUUID().toString();
        TranCreator usr0_tranCreator_1 = genUsrTranCreator(user0_creditCode, user0_cert_1);
        Transfer transfer = new Transfer(user0_creditCode, "12110107bi45jh675g", 5);
        Peer.Transaction tran_1 = usr0_tranCreator_1.createInvokeTran(tranId_1, usr0_cert_1.getId(), contractAssetsId, "transfer", JSON.toJSONString(transfer), 0, "");
        String tranHex_1 = Hex.encodeHexString(tran_1.toByteArray());
        postClient.postSignedTran(tranHex_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_1 = getTransactionResult(tranId_1);
        Peer.ActionResult actionResult_1 = tranResult_1.getErr();
        Assertions.assertEquals(102, actionResult_1.getCode(), "错误码为102");
        Assertions.assertEquals("余额不足", actionResult_1.getReason());
    }


    @Tag("修改证书状态")
    @Test
    @Order(6)
    @DisplayName("测试修改证书状态-usr0使用updateCertificateStatus修改一个不存在的证书状态")
    void testUpdateCertificateStatus_1() throws InterruptedException {
        String tranId = UUID.randomUUID().toString();
        JSONObject certStatus = new JSONObject();
        certStatus.fluentPut("creditCode", "usr-xxx").fluentPut("certName", user0_cert_1).fluentPut("state", false);
        Peer.Transaction tran = usr0_tranCreator_0.createInvokeTran(tranId, usr0_certId_0, didChaincodeId, updateCertificateStatus, certStatus.toJSONString(), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(102, actionResult.getCode(), "错误码为102");
        JSONObject errMsg = JSONObject.parseObject(actionResult.getReason());
        Assertions.assertEquals(13005, errMsg.getInteger("code"), "证书不存在");
        Assertions.assertEquals("证书不存在", errMsg.getString("reason"));
    }

    @Tag("修改证书状态")
    @Test
    @Order(7)
    @DisplayName("测试修改证书状态-usr0使用updateCertificateStatus，普通证书不能调用该方法")
    void testUpdateCertificateStatus_2() throws InterruptedException {
        String tranId = UUID.randomUUID().toString();
        JSONObject certStatus = new JSONObject();
        certStatus.fluentPut("creditCode", user0_creditCode).fluentPut("certName", user0_cert_1).fluentPut("state", false);
        Peer.Transaction tran = usr0_tranCreator_1.createInvokeTran(tranId, usr0_certId_1, didChaincodeId, updateCertificateStatus, certStatus.toJSONString(), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(102, actionResult.getCode(), "错误码为102");
        JSONObject errMsg = JSONObject.parseObject(actionResult.getReason());
        Assertions.assertEquals(13003, errMsg.getInteger("code"), "交易提交者非身份校验证书");
        Assertions.assertEquals("交易提交者非身份校验证书", errMsg.getString("reason"));
    }

    @Tag("修改证书状态")
    @Test
    @Order(8)
    @DisplayName("测试修改证书状态-usr0使用updateCertificateStatus，不能调用该方法修改身份证书")
    void testUpdateCertificateStatus_3() throws InterruptedException {
        String tranId = UUID.randomUUID().toString();
        JSONObject certStatus = new JSONObject();
        // 修改身份证书状态
        certStatus.fluentPut("creditCode", user0_creditCode).fluentPut("certName", user0_cert_0).fluentPut("state", false);
        Peer.Transaction tran = usr0_tranCreator_0.createInvokeTran(tranId, usr0_certId_0, didChaincodeId, updateCertificateStatus, certStatus.toJSONString(), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(102, actionResult.getCode(), "错误码为102");
        JSONObject errMsg = JSONObject.parseObject(actionResult.getReason());
        Assertions.assertEquals(13009, errMsg.getInteger("code"));
        Assertions.assertEquals("该方法不能修改身份证书状态，请通过 updateAllTypeCertificateStatus", errMsg.getString("reason"));
    }

    @Tag("修改证书状态")
    @Test
    @Order(9)
    @DisplayName("测试修改证书状态-usr0使用updateCertificateStatus, 修改成功")
    void testUpdateCertificateStatus_4() throws InterruptedException {
        String tranId = UUID.randomUUID().toString();
        JSONObject certStatus = new JSONObject();
        // 修改身份证书状态
        certStatus.fluentPut("creditCode", user0_creditCode).fluentPut("certName", user0_cert_1).fluentPut("state", false);
        Peer.Transaction tran = usr0_tranCreator_0.createInvokeTran(tranId, usr0_certId_0, didChaincodeId, updateCertificateStatus, certStatus.toJSONString(), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(0, tranResult.getErr().getCode(), "没有错误，修改成功");

        // step2: 使用注册好的证书对应的私钥提交签名交易 TODO 待完成,有问题需要解决, 下划线或者 . 分隔符不能正确解析， 且证书拿出来之后并未验证valid的有效性
        String tranId_1 = UUID.randomUUID().toString();
        Transfer transfer = new Transfer(user0_creditCode, "12110107bi45jh675g", 5);
        Peer.Transaction tran_1 = usr0_tranCreator_1.createInvokeTran(tranId_1, usr0_certId_1, contractAssetsId, "transfer", JSON.toJSONString(transfer), 0, "");
        String tranHex_1 = Hex.encodeHexString(tran_1.toByteArray());
        postClient.postSignedTran(tranHex_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_1 = getTransactionResult(tranId_1);
        Assertions.assertNull(tranResult_1, "本交易不会出块，因此不会出现在区块中");

        String tranId_2 = UUID.randomUUID().toString();
        // 修改身份证书状态
        certStatus.fluentPut("creditCode", user0_creditCode).fluentPut("certName", user0_cert_1).fluentPut("state", true);
        Peer.Transaction tran_2 = usr0_tranCreator_0.createInvokeTran(tranId_2, usr0_certId_0, didChaincodeId, updateCertificateStatus, certStatus.toJSONString(), 0, "");
        postClient.postSignedTran(tran_2);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_2 = getTransactionResult(tranId_2);
        Assertions.assertEquals(0, tranResult_2.getErr().getCode(), "没有错误，修改成功");
    }


    @Tag("注册任何证书")
    @Test
    @Order(10)
    @DisplayName("测试注册证书-usr0使用signUpAllTypeCertificate注册任意证书, 并提交交易, 该方法需要授权")
    void testSignUpAllTypeCert_1() throws IOException, InterruptedException {
        // step1: usr0为usr1注册普通证书, 没有权限, 会失败
        Peer.Certificate usr1_cert_1 = getUsrCert(user1_creditCode, user1_cert_1);
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = usr0_tranCreator_0.createInvokeTran(tranId, usr0_certId_0, didChaincodeId, signUpAllTypeCertificate, JsonFormat.printer().print(usr1_cert_1), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(101, actionResult.getCode(), "错误码为101");
        Assertions.assertEquals("没有找到授权的操作", actionResult.getReason());
        assertThat(actionResult.getReason()).isEqualTo("没有找到授权的操作");
    }

    @Tag("注册任何证书")
    @Test
    @Order(12)
    @DisplayName("测试注册证书-usr0使用signUpAllTypeCertificate注册任意证书, 并提交交易, 先使用node1授权, 再使用usr0为usr1注册普通证书")
    void testSignUpAllTypeCert_3() throws IOException, InterruptedException {
        // 授权，node1授权给usr0, 注册任意证书的方法
        long millis = System.currentTimeMillis();
        Peer.Authorize authorize = Peer.Authorize.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setGrant(node1_creditCode)
                .addGranted(user0_creditCode)
                .addOpId(DigestUtils.sha256Hex("RdidOperateAuthorizeTPL.signUpAllTypeCertificate"))
                .setIsTransfer(Peer.Authorize.TransferType.TRANSFER_REPEATEDLY)
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
                .setAuthorizeValid(true)
                .setVersion("1.0")
                .build();
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = node1Creator.createInvokeTran(tranId, node1CertId, didChaincodeId, grantOperate,
                JSONObject.toJSONString(Collections.singletonList(JsonFormat.printer().print(authorize))), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(0, actionResult.getCode(), "没有错误，授权成功");

        // step2: usr0为usr1 注册普通证书
        Peer.Certificate usr1_cert_1 = getUsrCert(user1_creditCode, user1_cert_1);
        String tranId_1 = UUID.randomUUID().toString();
        Peer.Transaction tran_1 = usr0_tranCreator_0.createInvokeTran(tranId_1, usr0_certId_0, didChaincodeId, signUpAllTypeCertificate, JsonFormat.printer().print(usr1_cert_1), 0, "");
        postClient.postSignedTran(tran_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_1 = getTransactionResult(tranId_1);
        Peer.ActionResult actionResult_1 = tranResult_1.getErr();
        Assertions.assertEquals(0, tranResult_1.getErr().getCode(), "没有错误，修改成功");
    }

    @Tag("注册任何证书")
    @Test
    @Order(13)
    @DisplayName("测试注册证书-usr0授权给usr1, 再使用usr1为usr2注册普通证书")
    void testSignUpAllTypeCert_4() throws IOException, InterruptedException {

        // step1: 注册证书, usr1为usr2注册普通证书, 没有权限调用
        Peer.Certificate usr2_cert_1 = getUsrCert(user2_creditCode, user2_cert_1);
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = usr1_tranCreator_0.createInvokeTran(tranId, usr1_certId_0, didChaincodeId, signUpAllTypeCertificate, JsonFormat.printer().print(usr2_cert_1), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(101, actionResult.getCode(), "错误码为101");
        Assertions.assertEquals("没有找到授权的操作", actionResult.getReason());

        // 授权，usr0授权给usr1注册任意证书的权限
        long millis = System.currentTimeMillis();
        Peer.Authorize authorize = Peer.Authorize.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setGrant(user0_creditCode)
                .addGranted(user1_creditCode)
                .addOpId(DigestUtils.sha256Hex("RdidOperateAuthorizeTPL.signUpAllTypeCertificate"))
                .setIsTransfer(Peer.Authorize.TransferType.TRANSFER_DISABLE)
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
                .setAuthorizeValid(true)
                .setVersion("1.0")
                .build();
        String tranId_1 = UUID.randomUUID().toString();
        Peer.Transaction tran_1 = usr0_tranCreator_1.createInvokeTran(tranId_1, usr0_certId_1, didChaincodeId, grantOperate,
                JSONObject.toJSONString(Collections.singletonList(JsonFormat.printer().print(authorize))), 0, "");
        postClient.postSignedTran(tran_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_1 = getTransactionResult(tranId_1);
        Peer.ActionResult actionResult_1 = tranResult_1.getErr();
        Assertions.assertEquals(0, actionResult_1.getCode(), "没有错误，授权成功");

        // step2: usr1为usr2注册证书, 虽然有权限, 但是usr2账户实体不存在，会失败
        String tranId_2 = UUID.randomUUID().toString();
        Peer.Transaction tran_2 = usr1_tranCreator_0.createInvokeTran(tranId_2, usr1_certId_0, didChaincodeId, signUpAllTypeCertificate, JsonFormat.printer().print(usr2_cert_1), 0, "");
        postClient.postSignedTran(tran_2);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_2 = getTransactionResult(tranId_2);
        Peer.ActionResult actionResult_2 = tranResult_2.getErr();
        Assertions.assertEquals(102, actionResult_2.getCode(), "错误码为102");
        JSONObject errMsg = JSONObject.parseObject(actionResult_2.getReason());
        Assertions.assertEquals(12003, errMsg.getInteger("code"), "Signer账户实体不存在");

        // step3: node1 为usr2注册账户实体
        String tranId_3 = UUID.randomUUID().toString();
        Peer.Signer signer_1 = usr2_signer.toBuilder().clearCertNames().clearAuthorizeIds().clearOperateIds().clearCredentialMetadataIds().build();
        Peer.Transaction tran_3 = node1Creator.createInvokeTran(tranId_3, node1CertId, didChaincodeId, signUpSigner, JsonFormat.printer().print(signer_1), 0, "");
        postClient.postSignedTran(tran_3);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_3 = getTransactionResult(tranId_3);
        Assertions.assertEquals(0, tranResult_3.getErr().getCode(), "没有错误，注册成功");

        // step4: 注册证书, usr1为usr2注册身份证书
        String tranId_4 = UUID.randomUUID().toString();
        // 构造一个身份证书
        Peer.Transaction tran_4 = usr1_tranCreator_0.createInvokeTran(tranId_4, usr1_certId_0, didChaincodeId, signUpAllTypeCertificate,
                JsonFormat.printer().print(usr2_cert_1.toBuilder().setCertType(Peer.Certificate.CertType.CERT_AUTHENTICATION).build()), 0, "");
        postClient.postSignedTran(tran_4);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_4 = getTransactionResult(tranId_4);
        Peer.ActionResult actionResult_4 = tranResult_4.getErr();
        Assertions.assertEquals(0, tranResult_4.getErr().getCode(), "没有错误，修改成功");

        // step5: 注册证书, usr1为usr2注册身份证书, 证书已存在
        String tranId_5 = UUID.randomUUID().toString();
        // 构造一个身份证书
        Peer.Transaction tran_5 = usr1_tranCreator_1.createInvokeTran(tranId_5, usr1_certId_1, didChaincodeId, signUpAllTypeCertificate,
                JsonFormat.printer().print(usr2_cert_1.toBuilder().setCertType(Peer.Certificate.CertType.CERT_AUTHENTICATION).build()), 0, "");
        postClient.postSignedTran(tran_5);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_5 = getTransactionResult(tranId_5);
        Peer.ActionResult actionResult_5 = tranResult_5.getErr();
        Assertions.assertEquals(102, actionResult_5.getCode(), "错误码为102");
        JSONObject errMsg_5 = JSONObject.parseObject(actionResult_5.getReason());
        Assertions.assertEquals(13007, errMsg_5.getInteger("code"), "用户的身份证书或者普通证书已存在");
        assertThat(errMsg_5.getString("reason")).isEqualTo("用户的身份证书或者普通证书已存在");

        //TODO queryLeveldb来查询身份证书和账户

    }

    @Tag("修改任何证书状态")
    @Test
    @Order(14)
    @DisplayName("测试修改任何证书状态-node1授权给usr1 修改任何证书的方法, 再使用usr1为usr2修改证书状态")
    void testUpdateAllTypeCertificateStatus_1() throws IOException, InterruptedException {

        // 授权，node1授权给usr1
        long millis = System.currentTimeMillis();
        Peer.Authorize authorize = Peer.Authorize.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setGrant(node1_creditCode)
                .addGranted(user1_creditCode)
                .addOpId(DigestUtils.sha256Hex("RdidOperateAuthorizeTPL.updateAllTypeCertificateStatus"))
                .setIsTransfer(Peer.Authorize.TransferType.TRANSFER_REPEATEDLY)
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
                .setAuthorizeValid(true)
                .setVersion("1.0")
                .build();
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = node1Creator.createInvokeTran(tranId, node1CertId, didChaincodeId, grantOperate,
                JSONObject.toJSONString(Collections.singletonList(JsonFormat.printer().print(authorize))), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(0, actionResult.getCode(), "没有错误，授权成功");

        // usr1为usr2更新证书(身份证书)状态
        String tranId_1 = UUID.randomUUID().toString();
        JSONObject certStatus = new JSONObject();
        certStatus.fluentPut("creditCode", user2_creditCode).fluentPut("certName", user2_cert_1).fluentPut("state", true);
        Peer.Transaction tran_1 = usr1_tranCreator_1.createInvokeTran(tranId_1, usr1_certId_1, didChaincodeId, updateAllTypeCertificateStatus, certStatus.toJSONString(), 0, "");
        postClient.postSignedTran(tran_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_1 = getTransactionResult(tranId_1);
        Assertions.assertEquals(0, tranResult_1.getErr().getCode(), "没有错误，修改成功");
    }


    @Tag("修改任何证书状态")
    @Test
    @Order(15)
    @DisplayName("测试修改任何证书状态-除superAdmin自己外的任何用户都不能修改superAdmin自己证书状态")
    void testUpdateAllTypeCertificateStatus_2() throws InterruptedException {

        // usr1为superAdmin更新证书状态, 会失败
        String tranId = UUID.randomUUID().toString();
        JSONObject certStatus = new JSONObject();
        certStatus.fluentPut("creditCode", super_creditCode).fluentPut("certName", "super_admin").fluentPut("state", false);
        Peer.Transaction tran = usr1_tranCreator_0.createInvokeTran(tranId, usr1_certId_0, didChaincodeId, updateAllTypeCertificateStatus, certStatus.toJSONString(), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(102, actionResult.getCode(), "错误码为102");
        JSONObject errMsg = JSONObject.parseObject(actionResult.getReason());
        Assertions.assertEquals(13006, errMsg.getInteger("code"), "非super_admin不能为super_admin注册certificate或修改certificate状态");
        assertThat(errMsg.getString("reason")).isEqualTo("非super_admin不能为super_admin注册certificate或修改certificate状态");
    }

}
