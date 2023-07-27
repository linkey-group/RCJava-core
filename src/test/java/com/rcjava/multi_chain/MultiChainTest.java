package com.rcjava.multi_chain;

import com.alibaba.fastjson2.JSONObject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import com.rcjava.client.ChainInfoClient;
import com.rcjava.client.TranPostClient;
import com.rcjava.did.DidTest;
import com.rcjava.protos.Peer;
import com.rcjava.tran.TranCreator;
import com.rcjava.tran.impl.CidStateTran;
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

    String host = "localhost:9081";
    TranPostClient postClient = new TranPostClient(host);
    ChainInfoClient infoClient = new ChainInfoClient(host);
    TranCreator usr0_tranCreator_0 = getUsr0_tranCreator_0();
    TranCreator usr1_tranCreator_0 = getUsr1_tranCreator_0();
    Peer.CertId usr0_certId_0 = getUsr0_certId_0();
    Peer.CertId usr1_certId_0 = getUsr1_certId_0();

    String cre_host = "localhost:9086";
    TranPostClient postCredenceClient = new TranPostClient(cre_host);
    ChainInfoClient infoCredenceClient = new ChainInfoClient(cre_host);

    public MultiChainTest() throws IOException {
    }

    @BeforeAll
    void init_1() {
        super_pri = CertUtil.genX509CertPrivateKey(new File("jks/test/multi_chain/identity.951002007l78123233.super_admin.jks"),
                "super_admin", "951002007l78123233.super_admin").getPrivateKey();
        System.out.println("jks/test/multi_chain/identity.951002007l78123233.super_admin.jks");
        superCreator = TranCreator.newBuilder().setPrivateKey(super_pri).setSignAlgorithm("SHA256withECDSA").build();
    }

    @Test
    @DisplayName("SuperAdmin注册用户usr-0，usr-1")
    @Order(1)
    void signUpSignerTest() throws InterruptedException, InvalidProtocolBufferException {
        Peer.Signer usr0_signer = this.getUsr0_signer();
        String tranId_1 = UUID.randomUUID().toString();
        usr0_signer = usr0_signer.toBuilder().clearCertNames().clearAuthorizeIds().clearOperateIds().clearCredentialMetadataIds().build();
        Peer.Transaction tran_1 = superCreator.createInvokeTran(tranId_1, superCertId, didChaincodeId, signUpSigner, JsonFormat.printer().print(usr0_signer), 0, "");
        postClient.postSignedTran(tran_1);
        Peer.ActionResult actionResult_1 = checkResult(tranId_1);
        Assertions.assertEquals(0, actionResult_1.getCode(), "没有错误，注册成功");

        Peer.Signer usr1_signer = this.getUsr1_signer();
        String tranId_2 = UUID.randomUUID().toString();
        usr1_signer = usr1_signer.toBuilder().clearCertNames().clearAuthorizeIds().clearOperateIds().clearCredentialMetadataIds().build();
        Peer.Transaction tran_2 = superCreator.createInvokeTran(tranId_2, superCertId, didChaincodeId, signUpSigner, JsonFormat.printer().print(usr1_signer), 0, "");
        postClient.postSignedTran(tran_2);
        Peer.ActionResult actionResult_2 = checkResult(tranId_2);
        Assertions.assertEquals(0, actionResult_2.getCode(), "没有错误，注册成功");
    }

    @Test
    @DisplayName("注册Operate-注册部署合约的权限")
    @Order(2)
    void signUpOperate() throws InterruptedException, IOException {

        Peer.ChaincodeId customTplId = Peer.ChaincodeId.newBuilder().setChaincodeName("CredenceTPL").setVersion(1).build();
        String tplString = FileUtils.readFileToString(new File("jks/test/tpl/CredenceTPL.scala"), StandardCharsets.UTF_8);
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
        Peer.ActionResult actionResult = checkCredenceResult(signedDeployTran.getId());
        Assertions.assertEquals(101, actionResult.getCode(), "错误码为101");
        assertThat(actionResult.getReason()).isIn(Arrays.asList("操作不存在", "没有找到授权的操作"));

        // step3: superAdmin注册部署合约A的操作
        long millis_1 = System.currentTimeMillis();
        Peer.Operate operate = Peer.Operate.newBuilder()
                .setOpId(DigestUtils.sha256Hex("credence-net:CredenceTPL.deploy"))
                .setDescription("发布合约-CredenceTPL")
                .setRegister(super_creditCode)
                .setIsPublish(false)
                .setOperateType(Peer.Operate.OperateType.OPERATE_CONTRACT)
                // 貌似没必要？
                .addAllOperateServiceName(Arrays.asList("transaction.stream", "transaction.postTranByString", "transaction.postTranStream", "transaction.postTran"))
                .setAuthFullName("credence-net:CredenceTPL.deploy")
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis_1 / 1000).setNanos((int) ((millis_1 % 1000) * 1000000)).build())
                .setOpValid(true)
                .setVersion("1.0")
                .build();
        String tranId_2 = UUID.randomUUID().toString();
        Peer.Transaction tran_2 = superCreator.createInvokeTran(tranId_2, superCertId, didChaincodeId, signUpOperate, JsonFormat.printer().print(operate), 0, "");
        postClient.postSignedTran(tran_2);
        Peer.ActionResult actionResult_2 = checkResult(tranId_2);
        Assertions.assertEquals(0, actionResult_2.getCode(), "没有错误，操作注册成功");

        // step4: superAdmin授权给usr0部署合约A的权限
        long millis = System.currentTimeMillis();
        Peer.Authorize authorize_1 = Peer.Authorize.newBuilder()
                .setId(did_network_id + UUID.randomUUID())
                .setGrant(super_creditCode)
                .addGranted(user0_creditCode_did)
                .addOpId(DigestUtils.sha256Hex("credence-net:CredenceTPL.deploy"))
                .setIsTransfer(Peer.Authorize.TransferType.TRANSFER_REPEATEDLY)
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
                .setAuthorizeValid(true)
                .setVersion("1.0")
                .build();

        String tranId_3 = UUID.randomUUID().toString();
        Peer.Transaction tran_3 = superCreator.createInvokeTran(tranId_3, superCertId, didChaincodeId, grantOperate,
                JSONObject.toJSONString(Collections.singletonList(JsonFormat.printer().print(authorize_1))), 0, "");
        postClient.postSignedTran(tran_3);
        Peer.ActionResult actionResult_3 = checkResult(tranId_3);
        Assertions.assertEquals(0, actionResult_3.getCode(), "没有错误，授权成功");

        // step5: usr0部署合约A成功
        DeployTran deployTran_1 = deployTran.toBuilder().setTxid(UUID.randomUUID().toString()).build();
        Peer.Transaction signedDeployTran_1 = usr0_tranCreator_0.createDeployTran(deployTran_1);
        postCredenceClient.postSignedTran(signedDeployTran_1);
        Peer.ActionResult actionResult_4 = checkCredenceResult(signedDeployTran_1.getId());
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
                "creProof3", String.format("{\"uuid\" : \"%s\",\"data\" : \"{\\\"data1\\\": \\\"xyb002\\\",\\\"data2\\\": \\\"xyb003\\\"}\"}", tranId_0), 0, "");
        String tranHex = Hex.encodeHexString(tran_0.toByteArray());
        postCredenceClient.postSignedTran(tranHex);
        Peer.ActionResult actionResult_0 = checkCredenceResult(tranId_0);
        Assertions.assertEquals(101, actionResult_0.getCode(), "错误码为101");
        assertThat(actionResult_0.getReason()).isIn(Arrays.asList("操作不存在", "部分操作不存在或者无效"));

        // step2: usr0注册合约A的某个方法的Operate成功
        long millis = System.currentTimeMillis();
        Peer.Operate operate = Peer.Operate.newBuilder()
                .setOpId(DigestUtils.sha256Hex("credence-net:CredenceTPL.creProof3"))
                .setDescription("测试注册合约某个方法")
                .setRegister(user0_creditCode_did)
                .setIsPublish(false)
                .setOperateType(Peer.Operate.OperateType.OPERATE_CONTRACT)
                // 貌似没必要？
                .addAllOperateServiceName(Arrays.asList("transaction.stream", "transaction.postTranByString", "transaction.postTranStream", "transaction.postTran"))
                .setAuthFullName("credence-net:CredenceTPL.creProof3")
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
                .setOpValid(true)
                .setVersion("1.0")
                .build();
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = usr0_tranCreator_0.createInvokeTran(tranId, usr0_certId_0, didChaincodeId, signUpOperate, JsonFormat.printer().print(operate), 0, "");
        postClient.postSignedTran(tran);
        Peer.ActionResult actionResult = checkResult(tranId);
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
                "creProof3", String.format("{\"uuid\" : \"%s\",\"data\" : \"{\\\"data1\\\": \\\"xyb002\\\",\\\"data2\\\": \\\"xyb003\\\"}\"}", tranId), 0, "");
        String tranHex = Hex.encodeHexString(tran.toByteArray());
        postCredenceClient.postSignedTran(tranHex);
        Peer.ActionResult actionResult = checkCredenceResult(tranId);
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
                "creProof3", String.format("{\"uuid\" : \"%s\",\"data\" : \"{\\\"data1\\\": \\\"xyb002\\\",\\\"data2\\\": \\\"xyb003\\\"}\"}", tranId), 0, "");
        String tranHex = Hex.encodeHexString(tran.toByteArray());
        postCredenceClient.postSignedTran(tranHex);
        Peer.ActionResult actionResult = checkCredenceResult(tranId);
        Assertions.assertEquals(101, actionResult.getCode(), "错误码为101");
        assertThat(actionResult.getReason()).isEqualTo("没有找到授权的操作");

    }

    @Test
    @Order(6)
    @DisplayName("授权-usr0授权赋予usr1调用CredenceProofTPL.creProof3的权限")
    void testGrantOperate() throws InterruptedException, InvalidProtocolBufferException {
        // step1: usr0授权赋予usr1调用CredenceProofTPL.creProof的权限
        String funcCreProofAuthId = "credence-net:credenceTpl-creProof3";
        long millis = System.currentTimeMillis();
        Peer.Authorize authorize_1 = Peer.Authorize.newBuilder()
                .setId(funcCreProofAuthId)
                .setGrant(user0_creditCode_did)
                .addGranted(user1_creditCode_did)
                .addOpId(DigestUtils.sha256Hex("credence-net:CredenceTPL.creProof3"))
                .setIsTransfer(Peer.Authorize.TransferType.TRANSFER_REPEATEDLY)
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
                .setAuthorizeValid(true)
                .setVersion("1.0")
                .build();
        String tranId_1 = UUID.randomUUID().toString();
        Peer.Transaction tran_1 = usr0_tranCreator_0.createInvokeTran(tranId_1, usr0_certId_0, didChaincodeId, grantOperate,
                JSONObject.toJSONString(Collections.singletonList(JsonFormat.printer().print(authorize_1))), 0, "");
        postClient.postSignedTran(tran_1);
        Peer.ActionResult actionResult_1 = checkResult(tranId_1);
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
                "creProof3", String.format("{\"uuid\" : \"%s\",\"data\" : \"{\\\"data1\\\": \\\"xyb002\\\",\\\"data2\\\": \\\"xyb003\\\"}\"}", tranId), 0, "");
        String tranHex = Hex.encodeHexString(tran.toByteArray());
        postCredenceClient.postSignedTran(tranHex);
        Peer.ActionResult actionResult = checkCredenceResult(tranId);
        Assertions.assertEquals(0, actionResult.getCode(), "存证成功");

    }

    @Test
    @Order(8)
    @DisplayName("禁用授权-usr0禁用CredenceProofTPL.creProof3，禁用赋予usr1的权限")
    void testUpdateGrantOperateStatus() throws InterruptedException {

        // step1: usr0禁用授权(CredenceProofTPL.creProof)
        String funcCreProofAuthId = "credence-net:credenceTpl-creProof3";
        JSONObject authStatus = new JSONObject();
        authStatus.put("authId", funcCreProofAuthId);
        authStatus.put("state", false);
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = usr0_tranCreator_0.createInvokeTran(tranId, usr0_certId_0, didChaincodeId, updateGrantOperateStatus, authStatus.toJSONString(), 0, "");
        postClient.postSignedTran(tran);
        Peer.ActionResult actionResult = checkResult(tranId);
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
                "creProof3", String.format("{\"uuid\" : \"%s\",\"data\" : \"{\\\"data1\\\": \\\"xyb002\\\",\\\"data2\\\": \\\"xyb003\\\"}\"}", tranId), 0, "");
        String tranHex = Hex.encodeHexString(tran.toByteArray());
        postCredenceClient.postSignedTran(tranHex);
        Peer.ActionResult actionResult = checkCredenceResult(tranId);
        Assertions.assertEquals(101, actionResult.getCode(), "错误码为101");
        assertThat(actionResult.getReason()).isEqualTo("授权已经失效");
    }

    @Test
    @DisplayName("禁用授权-usr0向业务网络调用DID合约，合约ID不存在")
    @Order(10)
    void testUpdateGrantOperateStatus_1() throws InterruptedException {
        // step1: usr0启用授权(CredenceProofTPL.creProof)
        String funcCreProofAuthId = "credence-net:credenceTpl-creProof3";
        JSONObject authStatus = new JSONObject();
        authStatus.put("authId", funcCreProofAuthId);
        authStatus.put("state", true);
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = usr0_tranCreator_0.createInvokeTran(tranId, usr0_certId_0, didChaincodeId, updateGrantOperateStatus, authStatus.toJSONString(), 0, "");
        postCredenceClient.postSignedTran(tran);
        Peer.ActionResult actionResult = checkCredenceResult(tranId);
        Assertions.assertEquals(105, actionResult.getCode(), "调用的chainCode不存在");
    }

    @Test
    @DisplayName("禁用授权-usr0向身份网络调用DID合约，启用成功")
    @Order(11)
    void testUpdateGrantOperateStatus_2() throws InterruptedException {
        // step1: usr0启用授权(CredenceProofTPL.creProof)
        String funcCreProofAuthId = "credence-net:credenceTpl-creProof3";
        JSONObject authStatus = new JSONObject();
        authStatus.put("authId", funcCreProofAuthId);
        authStatus.put("state", true);
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = usr0_tranCreator_0.createInvokeTran(tranId, usr0_certId_0, didChaincodeId, updateGrantOperateStatus, authStatus.toJSONString(), 0, "");
        postClient.postSignedTran(tran);
        Peer.ActionResult actionResult = checkResult(tranId);
        Assertions.assertEquals(0, actionResult.getCode(), "没有错误，启用授权成功");
    }

    @RepeatedTest(name = "重复修改状态", value =  2)
    @Order(12)
    @DisplayName("修改账户状态-管理员修改usr1的账户状态")
    void testUpdateSignerStatus() throws InterruptedException {
        // step1 superAdmin禁用usr1的账户
        String tranId = UUID.randomUUID().toString();
        JSONObject status = new JSONObject();
        status.fluentPut("creditCode", user1_creditCode_did);
        status.fluentPut("state", false);
        Peer.Transaction tran = superCreator.createInvokeTran(tranId, superCertId, didChaincodeId, updateSignerStatus, status.toJSONString(), 0, "");
        postClient.postSignedTran(tran);
        Peer.ActionResult actionResult = checkResult(tranId);
        Assertions.assertEquals(0, actionResult.getCode(), "没有错误，修改成功");

        // usr1提交交易失败
        Peer.ChaincodeId credenceTPLId = Peer.ChaincodeId.newBuilder().setChaincodeName("CredenceTPL").setVersion(1).build();
        String tranId_1 = UUID.randomUUID().toString();
        Peer.Transaction tran_1 = usr1_tranCreator_0.createInvokeTran(tranId_1, usr1_certId_0, credenceTPLId,
                "creProof3", String.format("{\"uuid\" : \"%s\",\"data\" : \"{\\\"data1\\\": \\\"xyb002\\\",\\\"data2\\\": \\\"xyb003\\\"}\"}", tranId_1), 0, "");
        String tranHex_1 = Hex.encodeHexString(tran_1.toByteArray());
        postCredenceClient.postSignedTran(tranHex_1);
        Peer.ActionResult actionResult_1 = checkCredenceResult(tranId_1);
        Assertions.assertEquals(101, actionResult_1.getCode(), "错误码为101");
        Assertions.assertEquals("实体账户已经失效", actionResult_1.getReason());

        // superAdmin启用usr1的账户
        String tranId_2 = UUID.randomUUID().toString();
        status.fluentPut("state", true);
        Peer.Transaction tran_2 = superCreator.createInvokeTran(tranId_2, superCertId, didChaincodeId, updateSignerStatus, status.toJSONString(), 0, "");
        postClient.postSignedTran(tran_2);
        Peer.ActionResult actionResult_2 = checkResult(tranId_2);
        Peer.TransactionResult tranResult_2 = infoClient.getTranResultByTranId(tranId_2);
        Assertions.assertEquals(tranId_2, tranResult_2.getTxId());
        Assertions.assertEquals(0, actionResult_2.getCode(), "没有错误，修改成功");

        // usr1提交交易成功
        String tranId_3 = UUID.randomUUID().toString();
        Peer.Transaction tran_3 = usr1_tranCreator_0.createInvokeTran(tranId_3, usr1_certId_0, credenceTPLId,
                "creProof3", String.format("{\"uuid\" : \"%s\",\"data\" : \"{\\\"data1\\\": \\\"xyb002\\\",\\\"data2\\\": \\\"xyb003\\\"}\"}", tranId_3), 0, "");
        String tranHex_3 = Hex.encodeHexString(tran_3.toByteArray());
        postCredenceClient.postSignedTran(tranHex_3);
        Peer.ActionResult actionResult_3 = checkCredenceResult(tranId_3);
        Assertions.assertEquals(0, actionResult_3.getCode(), "存证成功");
    }

    @Test
    @DisplayName("禁用合约")
    @Order(13)
    void testDisableContract() throws IOException, InterruptedException {
        Peer.ChaincodeId credenceTPLId = Peer.ChaincodeId.newBuilder().setChaincodeName("CredenceTPL").setVersion(1).build();
        CidStateTran stateTran = CidStateTran.newBuilder()
                .setTxid(UUID.randomUUID().toString())
                .setCertId(usr0_certId_0)
                .setChaincodeId(credenceTPLId)
                .setState(false)
                .build();
        // step1: usr0不可以部署合约
        Peer.Transaction signedStateTran = usr0_tranCreator_0.createCidStateTran(stateTran);
        postCredenceClient.postSignedTran(signedStateTran);
        Peer.ActionResult actionResult = checkCredenceResult(signedStateTran.getId());
        Assertions.assertEquals(101, actionResult.getCode(), "错误码为101");
        assertThat(actionResult.getReason()).isIn(Arrays.asList("操作不存在", "没有找到授权的操作"));

        // step2: superAdmin注册部署合约A的操作
        long millis_1 = System.currentTimeMillis();
        Peer.Operate operate = Peer.Operate.newBuilder()
                .setOpId(DigestUtils.sha256Hex("credence-net:CredenceTPL.setState"))
                .setDescription("修改合约状态-CredenceTPL")
                .setRegister(super_creditCode)
                .setIsPublish(false)
                .setOperateType(Peer.Operate.OperateType.OPERATE_CONTRACT)
                .setAuthFullName("credence-net:CredenceTPL.setState")
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis_1 / 1000).setNanos((int) ((millis_1 % 1000) * 1000000)).build())
                .setOpValid(true)
                .setVersion("1.0")
                .build();
        String tranId_2 = UUID.randomUUID().toString();
        Peer.Transaction tran_2 = superCreator.createInvokeTran(tranId_2, superCertId, didChaincodeId, signUpOperate, JsonFormat.printer().print(operate), 0, "");
        postClient.postSignedTran(tran_2);
        Peer.ActionResult actionResult_2 = checkResult(tranId_2);
        Assertions.assertEquals(0, actionResult_2.getCode(), "没有错误，操作注册成功");

        // step3: superAdmin授权给usr0部署合约A的权限
        long millis = System.currentTimeMillis();
        Peer.Authorize authorize_1 = Peer.Authorize.newBuilder()
                .setId(did_network_id + UUID.randomUUID())
                .setGrant(super_creditCode)
                .addGranted(user0_creditCode_did)
                .addOpId(DigestUtils.sha256Hex("credence-net:CredenceTPL.setState"))
                .setIsTransfer(Peer.Authorize.TransferType.TRANSFER_REPEATEDLY)
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
                .setAuthorizeValid(true)
                .setVersion("1.0")
                .build();
        String tranId_3 = UUID.randomUUID().toString();
        Peer.Transaction tran_3 = superCreator.createInvokeTran(tranId_3, superCertId, didChaincodeId, grantOperate,
                JSONObject.toJSONString(Collections.singletonList(JsonFormat.printer().print(authorize_1))), 0, "");
        postClient.postSignedTran(tran_3);
        Peer.ActionResult actionResult_3 = checkResult(tranId_3);
        Assertions.assertEquals(0, actionResult_3.getCode(), "没有错误，授权成功");

        // step4: usr0修改合约状态成功，禁用合约
        CidStateTran stateTran_1 = stateTran.toBuilder().setTxid(UUID.randomUUID().toString()).build();
        Peer.Transaction signedStateTran_1 = usr0_tranCreator_0.createCidStateTran(stateTran_1);
        postCredenceClient.postSignedTran(signedStateTran_1);
        Peer.ActionResult actionResult_4 = checkCredenceResult(signedStateTran_1.getId());
        Assertions.assertEquals(0, actionResult_4.getCode(), "没有错误，修改合约状态成功");

        // 会失败
        String tranId_5 = UUID.randomUUID().toString();
        Peer.Transaction tran_5 = usr0_tranCreator_0.createInvokeTran(tranId_5, usr0_certId_0, credenceTPLId,
                "creProof3", String.format("{\"uuid\" : \"%s\",\"data\" : \"{\\\"data1\\\": \\\"xyb002\\\",\\\"data2\\\": \\\"xyb003\\\"}\"}", tranId_5), 0, "");
        String tranHex_5 = Hex.encodeHexString(tran_5.toByteArray());
        postCredenceClient.postSignedTran(tranHex_5);
        Peer.ActionResult actionResult_5 = checkCredenceResult(tranId_5);
        Assertions.assertEquals(101, actionResult_5.getCode(), "错误码为101");
        Assertions.assertEquals("合约处于禁用状态", actionResult_5.getReason());

        // step5: usr0 授权给usr1
        String tranId_6 = UUID.randomUUID().toString();
        Peer.Authorize authorize_2 = authorize_1.toBuilder()
                .setId(did_network_id + UUID.randomUUID())
                .setGrant(user0_creditCode_did)
                .clearGranted().addGranted(user1_creditCode_did).build();
        Peer.Transaction tran_6 = usr0_tranCreator_0.createInvokeTran(tranId_6, usr0_certId_0, didChaincodeId, grantOperate,
                JSONObject.toJSONString(Collections.singletonList(JsonFormat.printer().print(authorize_2))), 0, "");
        postClient.postSignedTran(tran_6);
        Peer.ActionResult actionResult_6 = checkResult(tranId_6);
        Assertions.assertEquals(0, actionResult_6.getCode(), "没有错误，授权成功");

        // step6: usr1启用合约，启用合约
        CidStateTran stateTran_2 = stateTran.toBuilder().setTxid(UUID.randomUUID().toString()).setCertId(usr1_certId_0).setState(true).build();
        Peer.Transaction signedStateTran_2 = usr1_tranCreator_0.createCidStateTran(stateTran_2);
        postCredenceClient.postSignedTran(signedStateTran_2);
        Peer.ActionResult actionResult_7 = checkCredenceResult(signedStateTran_2.getId());
        Assertions.assertEquals(0, actionResult_7.getCode(), "没有错误，修改合约状态成功");

        // 会成功
        String tranId_8 = UUID.randomUUID().toString();
        Peer.Transaction tran_8 = usr0_tranCreator_0.createInvokeTran(tranId_8, usr0_certId_0, credenceTPLId,
                "creProof3", String.format("{\"uuid\" : \"%s\",\"data\" : \"{\\\"data1\\\": \\\"xyb002\\\",\\\"data2\\\": \\\"xyb003\\\"}\"}", tranId_8), 0, "");
        postCredenceClient.postSignedTran(tran_8);
        Peer.ActionResult actionResult_8 = checkCredenceResult(tranId_8);
        Assertions.assertEquals(0, actionResult_8.getCode(), "存证成功");

    }
}
