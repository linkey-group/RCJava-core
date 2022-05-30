package com.rcjava.multi_chain;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import com.rcjava.client.ChainInfoClient;
import com.rcjava.client.TranPostClient;
import com.rcjava.did.DidTest;
import com.rcjava.protos.Peer;
import com.rcjava.tran.TranCreator;
import com.rcjava.tran.impl.DeployTran;
import com.rcjava.util.CertUtil;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author zyf
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MultiChainTest extends DidTest {

    TranPostClient postClient = new TranPostClient("localhost:9081");
    ChainInfoClient infoClient = new ChainInfoClient("localhost:9081");
    TranCreator usr0_tranCreator_0 = getUsr0_tranCreator_0();
    TranCreator usr1_tranCreator_0 = getUsr1_tranCreator_0();
    Peer.CertId usr0_certId_0 = getUsr0_certId_0();
    Peer.CertId usr1_certId_0 = getUsr1_certId_0();

    TranPostClient postCredenceClient = new TranPostClient("localhost:8081");
    ChainInfoClient infoCredenceClient = new ChainInfoClient("localhost:8081");

    public MultiChainTest() throws IOException {
    }

    @BeforeAll
    void init_1() {
        super_pri = CertUtil.genX509CertPrivateKey(new File("jks/multi_chain/951002007l78123233.super_admin.jks"),
                "super_admin", "951002007l78123233.super_admin").getPrivateKey();
        System.out.println("jks/multi_chain/951002007l78123233.super_admin.jks");
        superCreator = TranCreator.newBuilder().setPrivateKey(super_pri).setSignAlgorithm("SHA256withECDSA").build();
    }

    @Test
    @DisplayName("SuperAdmin注册用户usr-1，usr-2")
    @Order(1)
    void signUpSignerTest() throws InterruptedException, InvalidProtocolBufferException {
        Peer.Signer usr0_signer = this.getUsr0_signer();
        String tranId_1 = UUID.randomUUID().toString();
        usr0_signer = usr0_signer.toBuilder().clearCertNames().clearAuthorizeIds().clearOperateIds().clearCredentialMetadataIds().build();
        Peer.Transaction tran_1 = superCreator.createInvokeTran(tranId_1, superCertId, didChaincodeId, signUpSigner, JsonFormat.printer().print(usr0_signer), 0, "");
        postClient.postSignedTran(tran_1);
        TimeUnit.SECONDS.sleep(5);
        Peer.TransactionResult tranResult_1 = infoClient.getTranResultByTranId(tranId_1);
        Assertions.assertEquals(0, tranResult_1.getErr().getCode(), "没有错误，注册成功");

        Peer.Signer usr1_signer = this.getUsr1_signer();
        String tranId_2 = UUID.randomUUID().toString();
        usr1_signer = usr1_signer.toBuilder().clearCertNames().clearAuthorizeIds().clearOperateIds().clearCredentialMetadataIds().build();
        Peer.Transaction tran_2 = superCreator.createInvokeTran(tranId_2, superCertId, didChaincodeId, signUpSigner, JsonFormat.printer().print(usr1_signer), 0, "");
        postClient.postSignedTran(tran_2);
        TimeUnit.SECONDS.sleep(5);
        Peer.TransactionResult tranResult_2 = infoClient.getTranResultByTranId(tranId_2);
        Assertions.assertEquals(0, tranResult_2.getErr().getCode(), "没有错误，注册成功");
    }

    @Test
    @DisplayName("注册Operate-注册合约")
    @Order(2)
    void signUpOperate() throws InterruptedException, IOException {

        Peer.ChaincodeId customTplId = Peer.ChaincodeId.newBuilder().setChaincodeName("CredenceTPL").setVersion(1).build();
        String tplString = FileUtils.readFileToString(new File("jks/did/tpl/CredenceTPL.scala"), StandardCharsets.UTF_8);
        Peer.ChaincodeDeploy chaincodeDeploy = Peer.ChaincodeDeploy.newBuilder()
                .setTimeout(5000)
                .setCodePackage(tplString)
                .setLegalProse("")
                .setCType(Peer.ChaincodeDeploy.CodeType.CODE_SCALA)
                .setRType(Peer.ChaincodeDeploy.RunType.RUN_SERIAL)
                .setSType(Peer.ChaincodeDeploy.StateType.STATE_BLOCK)
                .setInitParameter("")
                .setCclassification(Peer.ChaincodeDeploy.ContractClassification.CONTRACT_CUSTOM)
                .build();
        DeployTran deployTran = DeployTran.newBuilder()
                .setTxid(UUID.randomUUID().toString())
                .setCertId(usr0_certId_0)
                .setChaincodeId(customTplId)
                .setChaincodeDeploy(chaincodeDeploy)
                .build();
        // step2: usr0不可以部署合约
        deployTran = deployTran.toBuilder().setTxid(UUID.randomUUID().toString()).build();
        Peer.Transaction signedDeployTran = usr0_tranCreator_0.createDeployTran(deployTran);
        postCredenceClient.postSignedTran(signedDeployTran);
        TimeUnit.SECONDS.sleep(5);
        Peer.TransactionResult tranResult = infoCredenceClient.getTranResultByTranId(signedDeployTran.getId());
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(101, actionResult.getCode(), "错误码为101");
        assertThat(actionResult.getReason()).isIn(Arrays.asList("操作不存在", "没有找到授权的操作"));

        // step3: superAdmin注册部署合约A的操作
        long millis_1 = System.currentTimeMillis();
        Peer.Operate operate = Peer.Operate.newBuilder()
                .setOpId(DigestUtils.sha256Hex("CredenceTPL.deploy"))
                .setDescription("发布合约-CredenceTPL")
                .setRegister(super_creditCode)
                .setIsPublish(false)
                .setOperateType(Peer.Operate.OperateType.OPERATE_CONTRACT)
                // 貌似没必要？
                .addAllOperateServiceName(Arrays.asList("transaction.stream", "transaction.postTranByString", "transaction.postTranStream", "transaction.postTran"))
                .setAuthFullName("CredenceTPL.deploy")
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis_1 / 1000).setNanos((int) ((millis_1 % 1000) * 1000000)).build())
                .setOpValid(true)
                .setVersion("1.0")
                .build();
        String tranId_2 = UUID.randomUUID().toString();
        Peer.Transaction tran_2 = superCreator.createInvokeTran(tranId_2, superCertId, didChaincodeId, signUpOperate, JsonFormat.printer().print(operate), 0, "");
        postClient.postSignedTran(tran_2);
        TimeUnit.SECONDS.sleep(5);
        Peer.TransactionResult tranResult_2 = infoClient.getTranResultByTranId(tranId_2);
        Peer.ActionResult actionResult_2 = tranResult_2.getErr();
        Assertions.assertEquals(0, actionResult_2.getCode(), "没有错误，操作注册成功");

        // step4: superAdmin授权给usr0部署合约A的权限
        long millis = System.currentTimeMillis();
        Peer.Authorize authorize_1 = Peer.Authorize.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setGrant(super_creditCode)
                .addGranted(user0_creditCode)
                .addOpId(DigestUtils.sha256Hex("CredenceTPL.deploy"))
                .setIsTransfer(Peer.Authorize.TransferType.TRANSFER_REPEATEDLY)
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
                .setAuthorizeValid(true)
                .setVersion("1.0")
                .build();

        String tranId_3 = UUID.randomUUID().toString();
        Peer.Transaction tran_3 = superCreator.createInvokeTran(tranId_3, superCertId, didChaincodeId, grantOperate,
                JSONObject.toJSONString(Collections.singletonList(JsonFormat.printer().print(authorize_1))), 0, "");
        postClient.postSignedTran(tran_3);
        TimeUnit.SECONDS.sleep(5);
        Peer.TransactionResult tranResult_3 = infoClient.getTranResultByTranId(tranId_3);
        Peer.ActionResult actionResult_3 = tranResult_3.getErr();
        Assertions.assertEquals(0, actionResult_3.getCode(), "没有错误，授权成功");

        // step5: usr0部署合约A成功
        DeployTran deployTran_1 = deployTran.toBuilder().setTxid(UUID.randomUUID().toString()).build();
        Peer.Transaction signedDeployTran_1 = usr0_tranCreator_0.createDeployTran(deployTran_1);
        postCredenceClient.postSignedTran(signedDeployTran_1);
        TimeUnit.SECONDS.sleep(5);
        Peer.TransactionResult tranResult_4 = infoCredenceClient.getTranResultByTranId(signedDeployTran_1.getId());
        Peer.ActionResult actionResult_4 = tranResult_4.getErr();
        Assertions.assertEquals(0, actionResult_4.getCode(), "没有错误，合约部署成功");
    }

    @Test
    @DisplayName("注册Operate-注册合约的某个方法")
    @Order(3)
    void testSignUpOperate_1() throws InterruptedException, InvalidProtocolBufferException {

        // step1: usr0提交存证，失败
        Peer.ChaincodeId credenceTPLId = Peer.ChaincodeId.newBuilder().setChaincodeName("CredenceTPL").setVersion(1).build();
        String tranId_0 = UUID.randomUUID().toString();
        Peer.Transaction tran_0 = usr0_tranCreator_0.createInvokeTran(tranId_0, usr0_certId_0, credenceTPLId,
                "creProof", "{\"uuid\" : \"121000005l35120456\",\"data\" : \"{\\\"data1\\\": \\\"xyb002\\\",\\\"data2\\\": \\\"xyb003\\\"}\"}", 0, "");
        String tranHex = Hex.encodeHexString(tran_0.toByteArray());
        postCredenceClient.postSignedTran(tranHex);
        TimeUnit.SECONDS.sleep(5);
        Peer.TransactionResult tranResult_0 = infoCredenceClient.getTranResultByTranId(tranId_0);
        Peer.ActionResult actionResult_0 = tranResult_0.getErr();
        Assertions.assertEquals(101, actionResult_0.getCode(), "错误码为101");
        assertThat(actionResult_0.getReason()).isIn(Arrays.asList("操作不存在", "部分操作不存在或者无效"));

        // step2: usr0注册合约A的某个方法的Operate成功
        long millis = System.currentTimeMillis();
        Peer.Operate operate = Peer.Operate.newBuilder()
                .setOpId(DigestUtils.sha256Hex("CredenceTPL.creProof"))
                .setDescription("测试注册合约某个方法")
                .setRegister(user0_creditCode)
                .setIsPublish(false)
                .setOperateType(Peer.Operate.OperateType.OPERATE_CONTRACT)
                // 貌似没必要？
                .addAllOperateServiceName(Arrays.asList("transaction.stream", "transaction.postTranByString", "transaction.postTranStream", "transaction.postTran"))
                .setAuthFullName("CredenceTPL.creProof")
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
                .setOpValid(true)
                .setVersion("1.0")
                .build();
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = usr0_tranCreator_0.createInvokeTran(tranId, usr0_certId_0, didChaincodeId, signUpOperate, JsonFormat.printer().print(operate), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(5);
        Peer.TransactionResult tranResult = infoClient.getTranResultByTranId(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(0, actionResult.getCode(), "没有错误，操作注册成功");
    }

    @RepeatedTest(name = "多次提交存证", value = 5)
    @DisplayName("提交存证交易")
    @Order(4)
    void testCredenceProof() throws InterruptedException {
        // step1: usr0提交存证交易
        Peer.ChaincodeId credenceTPLId = Peer.ChaincodeId.newBuilder().setChaincodeName("CredenceTPL").setVersion(1).build();
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = usr0_tranCreator_0.createInvokeTran(tranId, usr0_certId_0, credenceTPLId,
                "creProof", "{\"uuid\" : \"121000005l35120456\",\"data\" : \"{\\\"data1\\\": \\\"xyb002\\\",\\\"data2\\\": \\\"xyb003\\\"}\"}", 0, "");
        String tranHex = Hex.encodeHexString(tran.toByteArray());
        postCredenceClient.postSignedTran(tranHex);
        TimeUnit.SECONDS.sleep(5);
        Peer.TransactionResult tranResult = infoCredenceClient.getTranResultByTranId(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(0, actionResult.getCode(), "存证成功");

    }

    @RepeatedTest(name = "多次提交存证", value = 2)
    @DisplayName("usr1提交存证交易")
    @Order(5)
    void testCredenceProof_1() throws InterruptedException {
        // step1: usr1提交存证交易，失败
        Peer.ChaincodeId credenceTPLId = Peer.ChaincodeId.newBuilder().setChaincodeName("CredenceTPL").setVersion(1).build();
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = usr1_tranCreator_0.createInvokeTran(tranId, usr1_certId_0, credenceTPLId,
                "creProof", "{\"uuid\" : \"121000005l35120456\",\"data\" : \"{\\\"data1\\\": \\\"xyb002\\\",\\\"data2\\\": \\\"xyb003\\\"}\"}", 0, "");
        String tranHex = Hex.encodeHexString(tran.toByteArray());
        postCredenceClient.postSignedTran(tranHex);
        TimeUnit.SECONDS.sleep(5);
        Peer.TransactionResult tranResult = infoCredenceClient.getTranResultByTranId(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(101, actionResult.getCode(), "错误码为101");
        assertThat(actionResult.getReason()).isEqualTo("没有找到授权的操作");

    }

    @Test
    @Order(6)
    @DisplayName("授权-usr0授权赋予usr1调用CredenceProofTPL.creProof的权限")
    void testGrantOperate() throws InterruptedException, InvalidProtocolBufferException {
        // step1: usr0授权赋予usr1调用CredenceProofTPL.creProof的权限
        String funcCreProofAuthId = "credenceTpl-creProof";
        long millis = System.currentTimeMillis();
        Peer.Authorize authorize_1 = Peer.Authorize.newBuilder()
                .setId(funcCreProofAuthId)
                .setGrant(user0_creditCode)
                .addGranted(user1_creditCode)
                .addOpId(DigestUtils.sha256Hex("CredenceTPL.creProof"))
                .setIsTransfer(Peer.Authorize.TransferType.TRANSFER_REPEATEDLY)
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
                .setAuthorizeValid(true)
                .setVersion("1.0")
                .build();
        String tranId_1 = UUID.randomUUID().toString();
        Peer.Transaction tran_1 = usr0_tranCreator_0.createInvokeTran(tranId_1, usr0_certId_0, didChaincodeId, grantOperate,
                JSONObject.toJSONString(Collections.singletonList(JsonFormat.printer().print(authorize_1))), 0, "");
        postClient.postSignedTran(tran_1);
        TimeUnit.SECONDS.sleep(5);
        Peer.TransactionResult tranResult_1 = infoClient.getTranResultByTranId(tranId_1);
        Peer.ActionResult actionResult_1 = tranResult_1.getErr();
        Assertions.assertEquals(0, actionResult_1.getCode(), "授权成功");
    }


    @RepeatedTest(name = "多次提交存证", value = 2)
    @DisplayName("usr1提交存证交易")
    @Order(7)
    void testCredenceProof_2() throws InterruptedException {
        // step1: usr1提交存证交易
        Peer.ChaincodeId credenceTPLId = Peer.ChaincodeId.newBuilder().setChaincodeName("CredenceTPL").setVersion(1).build();
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = usr1_tranCreator_0.createInvokeTran(tranId, usr1_certId_0, credenceTPLId,
                "creProof", "{\"uuid\" : \"121000005l35120456\",\"data\" : \"{\\\"data1\\\": \\\"xyb002\\\",\\\"data2\\\": \\\"xyb003\\\"}\"}", 0, "");
        String tranHex = Hex.encodeHexString(tran.toByteArray());
        postCredenceClient.postSignedTran(tranHex);
        TimeUnit.SECONDS.sleep(5);
        Peer.TransactionResult tranResult = infoCredenceClient.getTranResultByTranId(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(0, actionResult.getCode(), "存证成功");

    }

    @Test
    @Order(8)
    @DisplayName("禁用授权-usr0禁用CredenceProofTPL.creProof，禁用赋予usr1的权限")
    void testUpdateGrantOperateStatus() throws InterruptedException {

        // step1: usr0禁用授权(CredenceProofTPL.creProof)
        String funcCreProofAuthId = "credenceTpl-creProof";
        JSONObject authStatus = new JSONObject();
        authStatus.put("authId", funcCreProofAuthId);
        authStatus.put("state", false);
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = usr0_tranCreator_0.createInvokeTran(tranId, usr0_certId_0, didChaincodeId, updateGrantOperateStatus, authStatus.toJSONString(), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(5);
        Peer.TransactionResult tranResult = infoClient.getTranResultByTranId(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(0, actionResult.getCode(), "没有错误，禁用授权成功");
    }


    @RepeatedTest(name = "多次提交存证", value = 2)
    @DisplayName("usr1提交存证交易，授权无效")
    @Order(9)
    void testCredenceProof_3() throws InterruptedException {
        // step1: usr1提交存证交易
        Peer.ChaincodeId credenceTPLId = Peer.ChaincodeId.newBuilder().setChaincodeName("CredenceTPL").setVersion(1).build();
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = usr1_tranCreator_0.createInvokeTran(tranId, usr1_certId_0, credenceTPLId,
                "creProof", "{\"uuid\" : \"121000005l35120456\",\"data\" : \"{\\\"data1\\\": \\\"xyb002\\\",\\\"data2\\\": \\\"xyb003\\\"}\"}", 0, "");
        String tranHex = Hex.encodeHexString(tran.toByteArray());
        postCredenceClient.postSignedTran(tranHex);
        TimeUnit.SECONDS.sleep(5);
        Peer.TransactionResult tranResult = infoCredenceClient.getTranResultByTranId(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(101, actionResult.getCode(), "错误码为101");
        assertThat(actionResult.getReason()).isEqualTo("授权已经失效");
    }


}
