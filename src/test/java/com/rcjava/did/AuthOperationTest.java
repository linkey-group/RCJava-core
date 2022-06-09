package com.rcjava.did;

import com.alibaba.fastjson2.JSONObject;
import com.google.common.truth.Truth;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import com.rcjava.protos.Peer;
import com.rcjava.tran.impl.DeployTran;
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
public class AuthOperationTest extends DidTest{


    public AuthOperationTest() throws IOException {
    }

    String deployCreProof2AuthId = UUID.randomUUID().toString();
    String deployCreProof2AuthId_1 = UUID.randomUUID().toString();

    String funcCreProofAuthId = UUID.randomUUID().toString();
    String funcCreProof2AuthId = UUID.randomUUID().toString();

    @Test
    @Order(1)
    @DisplayName("授权-只能授权自有操作")
    void testGrantOperate_1() throws InterruptedException, InvalidProtocolBufferException {
        // step1: usr0 授权，但是grant设置为usr1
        long millis = System.currentTimeMillis();
        Peer.Authorize authorize = Peer.Authorize.newBuilder()
                .setId(deployCreProof2AuthId)
                .setGrant(user1_creditCode)
                .addGranted(user1_creditCode)
                .addOpId(DigestUtils.sha256Hex("identity-net.CredenceTPL.deploy"))
                .setIsTransfer(Peer.Authorize.TransferType.TRANSFER_REPEATEDLY)
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
                .setAuthorizeValid(true)
                .setVersion("1.0")
                .build();
        String tranId_1 = UUID.randomUUID().toString();
        Peer.Transaction tran_1 = usr0_tranCreator_0.createInvokeTran(tranId_1, usr0_certId_0, didChaincodeId, grantOperate,
                JSONObject.toJSONString(Collections.singletonList(JsonFormat.printer().print(authorize))), 0, "");
        postClient.postSignedTran(tran_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_1 = getTransactionResult(tranId_1);
        Peer.ActionResult actionResult_1 = tranResult_1.getErr();
        Assertions.assertEquals(102, actionResult_1.getCode(), "错误码为102");
        JSONObject errMsg_1 = JSONObject.parseObject(actionResult_1.getReason());
        Assertions.assertEquals(15005, errMsg_1.getInteger("code"), "签名交易提交者非权限的授权者");
    }

    @RepeatedTest(name = "重复授权, 本测试例中旧的AuthId会被删除", value = 2)
    @Order(2)
    @DisplayName("授权-usr0授权usr1部署CredenceProofTPL.deploy的权限, 可针对同一个操作对同一个人授权多次, 会覆盖掉先前的授权")
    void testGrantOperate_2() throws InterruptedException, InvalidProtocolBufferException {
        // step1: usr0授权usr1部署CredenceProofTPL.deploy的权限
        long millis = System.currentTimeMillis();
        Peer.Authorize authorize_1 = Peer.Authorize.newBuilder()
                .setId(deployCreProof2AuthId)
                .setGrant(user0_creditCode)
                .addGranted(user1_creditCode)
                .addOpId(DigestUtils.sha256Hex("identity-net.CredenceTPL.deploy"))
                .setIsTransfer(Peer.Authorize.TransferType.TRANSFER_REPEATEDLY)
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
                .setAuthorizeValid(true)
                .setVersion("1.0")
                .build();
        String tranId_1 = UUID.randomUUID().toString();
        Peer.Transaction tran_1 = usr0_tranCreator_0.createInvokeTran(tranId_1, usr0_certId_0, didChaincodeId, grantOperate,
                JSONObject.toJSONString(Collections.singletonList(JsonFormat.printer().print(authorize_1))), 0, "");
        postClient.postSignedTran(tran_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_1 = getTransactionResult(tranId_1);
        Peer.ActionResult actionResult_1 = tranResult_1.getErr();
        Assertions.assertEquals(0, actionResult_1.getCode(), "授权成功");

        // step2: 同一个授权ID，usr0授权给usr1
        String tranId_2 = UUID.randomUUID().toString();
        Peer.Authorize authorize_2 = authorize_1;
        Peer.Transaction tran_2 = usr0_tranCreator_0.createInvokeTran(tranId_2, usr0_certId_0, didChaincodeId, grantOperate,
                JSONObject.toJSONString(Collections.singletonList(JsonFormat.printer().print(authorize_2))), 0, "");
        postClient.postSignedTran(tran_2);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_2 = getTransactionResult(tranId_2);
        Peer.ActionResult actionResult_2 = tranResult_2.getErr();
        Assertions.assertEquals(102, actionResult_2.getCode(), "错误码为102");
        JSONObject errMsg_2 = JSONObject.parseObject(actionResult_2.getReason());
        Assertions.assertEquals(15002, errMsg_2.getInteger("code"), String.format("authId为%s的Authorize已经存在", authorize_2.getId()));
        Truth.assertThat(errMsg_2.getString("reason")).isEqualTo(String.format("authId为%s的Authorize已经存在", authorize_2.getId()));

        // step3: 同一个授权ID，usr0授权给usr2
        String tranId_3 = UUID.randomUUID().toString();
        Peer.Authorize authorize_3 = authorize_1.toBuilder()
                .clearGranted().addGranted(user2_creditCode)
                .build();
        Peer.Transaction tran_3 = usr0_tranCreator_0.createInvokeTran(tranId_3, usr0_certId_0, didChaincodeId, grantOperate,
                JSONObject.toJSONString(Collections.singletonList(JsonFormat.printer().print(authorize_3))), 0, "");
        postClient.postSignedTran(tran_3);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_3 = getTransactionResult(tranId_3);
        Peer.ActionResult actionResult_3 = tranResult_3.getErr();
        Assertions.assertEquals(102, actionResult_3.getCode(), "错误码为102");
        JSONObject errMsg_3 = JSONObject.parseObject(actionResult_3.getReason());
        Assertions.assertEquals(15002, errMsg_3.getInteger("code"), String.format("authId为%s的Authorize已经存在", authorize_3.getId()));
        Truth.assertThat(errMsg_3.getString("reason")).isEqualTo(String.format("authId为%s的Authorize已经存在", authorize_3.getId()));

        // step4: usr0授权usr1部署CredenceProofTPL.deploy的权限，不同ID重复授权（AuthId不一致，OperId是同一个）
        String tranId_4 = UUID.randomUUID().toString();
        Peer.Authorize authorize_4 = authorize_1.toBuilder().setId(deployCreProof2AuthId_1).build();
        Peer.Transaction tran_4 = usr0_tranCreator_0.createInvokeTran(tranId_4, usr0_certId_0, didChaincodeId, grantOperate,
                JSONObject.toJSONString(Collections.singletonList(JsonFormat.printer().print(authorize_4))), 0, "");
        postClient.postSignedTran(tran_4);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_4 = getTransactionResult(tranId_4);
        Peer.ActionResult actionResult_4 = tranResult_4.getErr();
        Assertions.assertEquals(0, actionResult_4.getCode(), "授权成功");
        // 因为Auth包含了同一个Operate，因此删除了上一个AuthId
        Truth.assertThat(tranResult_4.containsStatesDel("identity-net_RdidOperateAuthorizeTPL___auth-" + deployCreProof2AuthId)).isTrue();

    }

    @Test
    @Order(3)
    @DisplayName("升级合约-usr1注册操作CredenceTPL.creProof2")
    void testSignUpOperate_1() throws IOException, InterruptedException {
        // step1: <usr1>部署版本CredenceProofTPL-2的合约, 版本号为<2>
        Peer.ChaincodeId customTplId = Peer.ChaincodeId.newBuilder().setChaincodeName("CredenceTPL").setVersion(2).build();
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
                .setCertId(usr1_certId_0)
                .setChaincodeId(customTplId)
                .setChaincodeDeploy(chaincodeDeploy)
                .build();
        Peer.Transaction signedDeployTran = usr1_tranCreator_0.createDeployTran(deployTran);
        postClient.postSignedTran(signedDeployTran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(signedDeployTran.getId());
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(0, actionResult.getCode(), "没有错误，合约 CredenceTPL-2 部署成功");

        // step2: usr0能注册合约CredenceProof.creProof2的操作，此处不注册
        long millis = System.currentTimeMillis();
        Peer.Operate operate = Peer.Operate.newBuilder()
                .setOpId(DigestUtils.sha256Hex("identity-net.CredenceTPL.creProof2"))
                .setDescription("测试注册合约某个方法")
                .setRegister(user1_creditCode)
                .setIsPublish(false)
                .setOperateType(Peer.Operate.OperateType.OPERATE_CONTRACT)
                // 貌似没必要？
                .addAllOperateServiceName(Arrays.asList("transaction.stream", "transaction.postTranByString", "transaction.postTranStream", "transaction.postTran"))
                .setAuthFullName("identity-net.CredenceTPL.creProof2")
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
                .setOpValid(true)
                .setVersion("1.0")
                .build();

        // step3: usr1能注册合约CredenceProof.creProof2的操作
        String tranId_1 = UUID.randomUUID().toString();
        Peer.Transaction tran_1 = usr1_tranCreator_0.createInvokeTran(tranId_1, usr1_certId_0, didChaincodeId, signUpOperate, JsonFormat.printer().print(operate), 0, "");
        postClient.postSignedTran(tran_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_1 = getTransactionResult(tranId_1);
        Peer.ActionResult actionResult_1 = tranResult_1.getErr();
        Assertions.assertEquals(0, actionResult_1.getCode(), "没有错误，操作注册成功");

    }

    @Test
    @Order(4)
    @DisplayName("禁用授权-usr0禁用赋予usr1部署CredenceProofTPL.deploy的权限")
    void testUpdateGrantOperateStatus_1() throws InterruptedException, InvalidProtocolBufferException {
        // step1: usr0禁用赋予usr1部署CredenceProofTPL.deploy的权限
        JSONObject authStatus = new JSONObject();
//        authStatus.put("authId", "edb8bad3-2d2c-4bef-bc0f-93a297571ac6");
        authStatus.put("authId", deployCreProof2AuthId_1);
        authStatus.put("state", false);
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = usr0_tranCreator_0.createInvokeTran(tranId, usr0_certId_0, didChaincodeId, updateGrantOperateStatus, authStatus.toJSONString(), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(0, actionResult.getCode(), "没有错误，禁用授权成功");

        // step2: usr1不能继续让渡该权限给usr2 （包含授权，无效，因此不能让渡）
        long millis = System.currentTimeMillis();
        Peer.Authorize authorize_1 = Peer.Authorize.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setGrant(user1_creditCode)
                .addGranted(user2_creditCode)
                .addOpId(DigestUtils.sha256Hex("identity-net.CredenceTPL.deploy"))
                .setIsTransfer(Peer.Authorize.TransferType.TRANSFER_REPEATEDLY)
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
                .setAuthorizeValid(true)
                .setVersion("1.0")
                .build();
        String tranId_1 = UUID.randomUUID().toString();
        Peer.Transaction tran_1 = usr1_tranCreator_0.createInvokeTran(tranId_1, usr1_certId_0, didChaincodeId, grantOperate,
                JSONObject.toJSONString(Collections.singletonList(JsonFormat.printer().print(authorize_1))), 0, "");
        postClient.postSignedTran(tran_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_1 = getTransactionResult(tranId_1);
        Peer.ActionResult actionResult_1 = tranResult_1.getErr();
        Assertions.assertEquals(102, actionResult_1.getCode(), "错误码为102");
        JSONObject errMsg_1 = JSONObject.parseObject(actionResult_1.getReason());
        Assertions.assertEquals(15001, errMsg_1.getInteger("code"), "部分操作不存在或者无效");

        // 启用，可继续授权
        authStatus.put("authId", deployCreProof2AuthId_1);
        authStatus.put("state", true);
        String tranId_2 = UUID.randomUUID().toString();
        Peer.Transaction tran_2 = usr0_tranCreator_0.createInvokeTran(tranId_2, usr0_certId_0, didChaincodeId, updateGrantOperateStatus, authStatus.toJSONString(), 0, "");
        postClient.postSignedTran(tran_2);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_2 = getTransactionResult(tranId_2);
        Peer.ActionResult actionResult_2 = tranResult_2.getErr();
        Assertions.assertEquals(0, actionResult_2.getCode(), "没有错误，启用授权成功");

        String tranId_3 = UUID.randomUUID().toString();
        Peer.Transaction tran_3 = usr1_tranCreator_0.createInvokeTran(tranId_3, usr1_certId_0, didChaincodeId, grantOperate,
                JSONObject.toJSONString(Collections.singletonList(JsonFormat.printer().print(authorize_1))), 0, "");
        postClient.postSignedTran(tran_3);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_3 = getTransactionResult(tranId_3);
        Peer.ActionResult actionResult_3 = tranResult_3.getErr();
        Assertions.assertEquals(0, actionResult_3.getCode(), "没有错误，usr1授权给usr2成功");

    }

    @Test
    @Order(5)
    @DisplayName("授权-usr0授权赋予usr1调用CredenceProofTPL.creProof的权限")
    void testGrantOperate_3() throws InterruptedException, InvalidProtocolBufferException {

        // usr1 虽然是合约部署者，但是不能注册该方法了，因为已经注册过了
        long millis_0 = System.currentTimeMillis();
        Peer.Operate operate = Peer.Operate.newBuilder()
                .setOpId(DigestUtils.sha256Hex("identity-net.CredenceTPL.creProof"))
                .setDescription("测试注册合约某个方法")
                .setRegister(user1_creditCode)
                .setIsPublish(false)
                .setOperateType(Peer.Operate.OperateType.OPERATE_CONTRACT)
                // 貌似没必要？
                .addAllOperateServiceName(Arrays.asList("transaction.stream", "transaction.postTranByString", "transaction.postTranStream", "transaction.postTran"))
                .setAuthFullName("identity-net.CredenceTPL.creProof")
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis_0 / 1000).setNanos((int) ((millis_0 % 1000) * 1000000)).build())
                .setOpValid(true)
                .setVersion("1.0")
                .build();
        String tranId_0 = UUID.randomUUID().toString();
        Peer.Transaction tran_0 = usr1_tranCreator_0.createInvokeTran(tranId_0, usr1_certId_0, didChaincodeId, signUpOperate, JsonFormat.printer().print(operate), 0, "");
        postClient.postSignedTran(tran_0);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_0 = getTransactionResult(tranId_0);
        Peer.ActionResult actionResult_0 = tranResult_0.getErr();
        Assertions.assertEquals(102, actionResult_0.getCode(), "错误码为102");
        JSONObject errMsg_0 = JSONObject.parseObject(actionResult_0.getReason());
        Assertions.assertEquals(14001, errMsg_0.getInteger("code"), "operate已存在");

        // usr1不能调用CredenceProofTPL-2.creProof
        Peer.ChaincodeId credenceTPLId = Peer.ChaincodeId.newBuilder().setChaincodeName("CredenceTPL").setVersion(2).build();
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = usr1_tranCreator_0.createInvokeTran(tranId, usr1_certId_0, credenceTPLId,
                "creProof", "{\"uuid\" : \"121000005l35120456\",\"data\" : \"{\\\"data1\\\": \\\"xyb002\\\",\\\"data2\\\": \\\"xyb003\\\"}\"}", 0, "");
        String tranHex = Hex.encodeHexString(tran.toByteArray());
        postClient.postSignedTran(tranHex);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(101, actionResult.getCode(), "错误码为101");
        assertThat(actionResult.getReason()).isIn(Arrays.asList("操作不存在", "没有找到授权的操作"));

        // step1: usr0授权赋予usr1 和 usr2 调用CredenceProofTPL.creProof的权限
        long millis = System.currentTimeMillis();
        Peer.Authorize authorize_1 = Peer.Authorize.newBuilder()
                .setId(funcCreProofAuthId)
                .setGrant(user0_creditCode)
                .addAllGranted(Arrays.asList(user1_creditCode, user2_creditCode))
                .addOpId(DigestUtils.sha256Hex("identity-net.CredenceTPL.creProof"))
                .setIsTransfer(Peer.Authorize.TransferType.TRANSFER_REPEATEDLY)
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
                .setAuthorizeValid(true)
                .setVersion("1.0")
                .build();
        String tranId_1 = UUID.randomUUID().toString();
        Peer.Transaction tran_1 = usr0_tranCreator_0.createInvokeTran(tranId_1, usr0_certId_0, didChaincodeId, grantOperate,
                JSONObject.toJSONString(Collections.singletonList(JsonFormat.printer().print(authorize_1))), 0, "");
        postClient.postSignedTran(tran_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_1 = getTransactionResult(tranId_1);
        Peer.ActionResult actionResult_1 = tranResult_1.getErr();
        Assertions.assertEquals(0, actionResult_1.getCode(), "授权成功");

        // step2: usr1可以调用CredenceProofTPL<2>.creProof了
        String tranId_2 = UUID.randomUUID().toString();
        Peer.Transaction tran_2 = usr1_tranCreator_0.createInvokeTran(tranId_2, usr1_certId_0, credenceTPLId,
                "creProof", "{\"uuid\" : \"121000005l35120456\",\"data\" : \"{\\\"data1\\\": \\\"xyb002\\\",\\\"data2\\\": \\\"xyb003\\\"}\"}", 0, "");
        postClient.postSignedTran(tran_2);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_2 = getTransactionResult(tranId_2);
        Peer.ActionResult actionResult_2 = tranResult_2.getErr();
        Assertions.assertEquals(0, actionResult_2.getCode(), "usr1存证成功");

        // step3: usr2可以调用CredenceProofTPL<1>.creProof了
        String tranId_3 = UUID.randomUUID().toString();
        Peer.ChaincodeId credenceTPLId_1 = Peer.ChaincodeId.newBuilder().setChaincodeName("CredenceTPL").setVersion(1).build();
        Peer.Transaction tran_3 = usr2_tranCreator_0.createInvokeTran(tranId_3, usr2_certId_0, credenceTPLId_1,
                "creProof", "{\"uuid\" : \"121000005l35120456\",\"data\" : \"{\\\"data1\\\": \\\"xyb002\\\",\\\"data2\\\": \\\"xyb003\\\"}\"}", 0, "");
        postClient.postSignedTran(tran_3);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_3 = getTransactionResult(tranId_3);
        Peer.ActionResult actionResult_3 = tranResult_3.getErr();
        Assertions.assertEquals(0, actionResult_3.getCode(), "usr2存证成功");

    }

    @Test
    @Order(6)
    @DisplayName("禁用操作-usr0禁用CredenceProofTPL.creProof的操作")
    void testUpdateOperateStatus_1() throws InterruptedException, InvalidProtocolBufferException {

        // step1: usr0禁用CredenceProofTPL.creProof的操作
        String tranId_1 = UUID.randomUUID().toString();
        JSONObject operStatus = new JSONObject();
        operStatus.fluentPut("opId", DigestUtils.sha256Hex("identity-net.CredenceTPL.creProof")).fluentPut("state", false);
        Peer.Transaction tran_1 = usr0_tranCreator_0.createInvokeTran(tranId_1, usr0_certId_0, didChaincodeId, updateOperateStatus, operStatus.toJSONString(), 0, "");
        postClient.postSignedTran(tran_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_1 = getTransactionResult(tranId_1);
        Peer.ActionResult actionResult_1 = tranResult_1.getErr();
        Assertions.assertEquals(0, actionResult_1.getCode(), "没有错误，禁用操作成功");

        // step2: usr1 不可以调用CredenceProofTPL<2>.creProof了; usr2 不可以调用CredenceProofTPL<1>.creProof了
        String tranId_2 = UUID.randomUUID().toString();
        Peer.ChaincodeId credenceTPLId = Peer.ChaincodeId.newBuilder().setChaincodeName("CredenceTPL").setVersion(2).build();
        Peer.Transaction tran_2 = usr1_tranCreator_0.createInvokeTran(tranId_2, usr1_certId_0, credenceTPLId,
                "creProof", "{\"uuid\" : \"121000005l35120456\",\"data\" : \"{\\\"data1\\\": \\\"xyb002\\\",\\\"data2\\\": \\\"xyb003\\\"}\"}", 0, "");
        postClient.postSignedTran(tran_2);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_2 = getTransactionResult(tranId_2);
        Peer.ActionResult actionResult_2 = tranResult_2.getErr();
        Assertions.assertEquals(101, actionResult_2.getCode(), "错误码为101");
        assertThat(actionResult_2.getReason()).isEqualTo("操作已经失效");

        String tranId_3 = UUID.randomUUID().toString();
        Peer.ChaincodeId credenceTPLId_1 = Peer.ChaincodeId.newBuilder().setChaincodeName("CredenceTPL").setVersion(1).build();
        Peer.Transaction tran_3 = usr2_tranCreator_0.createInvokeTran(tranId_3, usr2_certId_0, credenceTPLId_1,
                "creProof", "{\"uuid\" : \"121000005l35120456\",\"data\" : \"{\\\"data1\\\": \\\"xyb002\\\",\\\"data2\\\": \\\"xyb003\\\"}\"}", 0, "");
        postClient.postSignedTran(tran_3);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_3 = getTransactionResult(tranId_3);
        Peer.ActionResult actionResult_3 = tranResult_3.getErr();
        Assertions.assertEquals(101, actionResult_3.getCode(), "错误码为101");
        assertThat(actionResult_3.getReason()).isEqualTo("操作已经失效");

        // step3: usr1不能授权给usr2调用CredenceProofTPL.creProof的权限（包含授权，有效，但是其中的操作无效，因此不能让渡）
        long millis = System.currentTimeMillis();
        Peer.Authorize authorize_1 = Peer.Authorize.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setGrant(user1_creditCode)
                .addGranted(user2_creditCode)
                .addOpId(DigestUtils.sha256Hex("identity-net.CredenceTPL.creProof"))
                .setIsTransfer(Peer.Authorize.TransferType.TRANSFER_REPEATEDLY)
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
                .setAuthorizeValid(true)
                .setVersion("1.0")
                .build();
        String tranId_4 = UUID.randomUUID().toString();
        Peer.Transaction tran_4 = usr1_tranCreator_0.createInvokeTran(tranId_4, usr1_certId_0, didChaincodeId, grantOperate,
                JSONObject.toJSONString(Collections.singletonList(JsonFormat.printer().print(authorize_1))), 0, "");
        postClient.postSignedTran(tran_4);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_4 = getTransactionResult(tranId_4);
        Peer.ActionResult actionResult_4 = tranResult_4.getErr();
        Assertions.assertEquals(102, actionResult_4.getCode(), "错误码为102");
        JSONObject errMsg_4 = JSONObject.parseObject(actionResult_4.getReason());
        Assertions.assertEquals(15001, errMsg_4.getInteger("code"), "部分操作不存在或者无效");

        // step4: 启用操作
        String tranId_5 = UUID.randomUUID().toString();
        operStatus.fluentPut("opId", DigestUtils.sha256Hex("identity-net.CredenceTPL.creProof")).fluentPut("state", true);
        Peer.Transaction tran_5 = usr0_tranCreator_0.createInvokeTran(tranId_5, usr0_certId_0, didChaincodeId, updateOperateStatus, operStatus.toJSONString(), 0, "");
        postClient.postSignedTran(tran_5);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_5 = getTransactionResult(tranId_5);
        Peer.ActionResult actionResult_5 = tranResult_5.getErr();
        Assertions.assertEquals(0, actionResult_5.getCode(), "没有错误，启用操作成功");

    }

    @Test
    @Order(7)
    @DisplayName("授权操作-usr1授权CredenceProofTPL.creProof2的操作给usr0，usr1授权给usr2")
    void testUpdateGrantOperateStatus_2() throws InterruptedException, InvalidProtocolBufferException {
        // step1: usr1授权CredenceProofTPL.creProof2的操作给usr0
        long millis = System.currentTimeMillis();
        Peer.Authorize authorize_1 = Peer.Authorize.newBuilder()
                .setId(funcCreProof2AuthId)
                .setGrant(user1_creditCode)
                .addGranted(user0_creditCode)
                .addOpId(DigestUtils.sha256Hex("identity-net.CredenceTPL.creProof2"))
                .setIsTransfer(Peer.Authorize.TransferType.TRANSFER_REPEATEDLY)
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
                .setAuthorizeValid(true)
                .setVersion("1.0")
                .build();
        String tranId_1 = UUID.randomUUID().toString();
        Peer.Transaction tran_1 = usr1_tranCreator_0.createInvokeTran(tranId_1, usr1_certId_0, didChaincodeId, grantOperate,
                JSONObject.toJSONString(Collections.singletonList(JsonFormat.printer().print(authorize_1))), 0, "");
        postClient.postSignedTran(tran_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_1 = getTransactionResult(tranId_1);
        Peer.ActionResult actionResult_1 = tranResult_1.getErr();
        Assertions.assertEquals(0, actionResult_1.getCode(), "没有错误，授权给usr0成功");

        // step2: usr1授权(CredenceProofTPL.creProof2)让渡给usr2
        long millis_2 = System.currentTimeMillis();
        Peer.Authorize authorize_2 = Peer.Authorize.newBuilder()
                .setId(UUID.randomUUID().toString())
                .setGrant(user0_creditCode)
                .addGranted(user2_creditCode)
                .addOpId(DigestUtils.sha256Hex("identity-net.CredenceTPL.creProof2"))
                .setIsTransfer(Peer.Authorize.TransferType.TRANSFER_REPEATEDLY)
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis_2 / 1000).setNanos((int) ((millis_2 % 1000) * 1000000)).build())
                .setAuthorizeValid(true)
                .setVersion("1.0")
                .build();
        String tranId_2 = UUID.randomUUID().toString();
        Peer.Transaction tran_2 = usr0_tranCreator_0.createInvokeTran(tranId_2, usr0_certId_0, didChaincodeId, grantOperate,
                JSONObject.toJSONString(Collections.singletonList(JsonFormat.printer().print(authorize_2))), 0, "");
        postClient.postSignedTran(tran_2);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_2 = getTransactionResult(tranId_2);
        Peer.ActionResult actionResult_2 = tranResult_2.getErr();
        Assertions.assertEquals(0, actionResult_2.getCode(), "没有错误，授权给usr2成功");

    }


    @Test
    @Order(8)
    @DisplayName("禁用授权-不具有传递性")
    void testUpdateGrantOperateStatus_3() throws InterruptedException, InvalidProtocolBufferException {
        // step1: usr1禁用授权(CredenceProofTPL.creProof2)
        JSONObject authStatus = new JSONObject();
        authStatus.put("authId", funcCreProof2AuthId);
        authStatus.put("state", false);
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = usr1_tranCreator_0.createInvokeTran(tranId, usr1_certId_0, didChaincodeId, updateGrantOperateStatus, authStatus.toJSONString(), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(0, actionResult.getCode(), "没有错误，禁用授权成功");

        // step2: usr0不能调用CredenceProofTPL.creProof2的操作
        String tranId_2 = UUID.randomUUID().toString();
        Peer.ChaincodeId credenceTPLId = Peer.ChaincodeId.newBuilder().setChaincodeName("CredenceTPL").setVersion(2).build();
        Peer.Transaction tran_2 = usr0_tranCreator_0.createInvokeTran(tranId_2, usr0_certId_0, credenceTPLId,
                "creProof2", "{\"uuid\" : \"121000005l35120456\",\"data\" : \"{\\\"data1\\\": \\\"xyb002\\\",\\\"data2\\\": \\\"xyb003\\\"}\"}", 0, "");
        postClient.postSignedTran(tran_2);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_2 = getTransactionResult(tranId_2);
        Peer.ActionResult actionResult_2 = tranResult_2.getErr();
        Assertions.assertEquals(101, actionResult_2.getCode(), "错误码为101");
        assertThat(actionResult_2.getReason()).isEqualTo("授权已经失效");

        // step2: usr2仍然能调用, 授权禁用不具有传递性
        String tranId_3 = UUID.randomUUID().toString();
        Peer.ChaincodeId credenceTPLId_1 = Peer.ChaincodeId.newBuilder().setChaincodeName("CredenceTPL").setVersion(1).build();
        Peer.Transaction tran_3 = usr2_tranCreator_0.createInvokeTran(tranId_3, usr2_certId_0, credenceTPLId_1,
                "creProof2", "{\"uuid\" : \"121000005l35120456\",\"data\" : \"{\\\"data1\\\": \\\"xyb002\\\",\\\"data2\\\": \\\"xyb003\\\"}\"}", 0, "");
        postClient.postSignedTran(tran_3);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_3 = getTransactionResult(tranId_3);
        Peer.ActionResult actionResult_3 = tranResult_3.getErr();
        Assertions.assertEquals(0, actionResult_3.getCode(), "没有错误，仍然可以调用");

        // step3: usr1启用授权(CredenceProofTPL.creProof2)
        String tranId_4 = UUID.randomUUID().toString();
        authStatus.put("state", true);
        Peer.Transaction tran_4 = usr1_tranCreator_0.createInvokeTran(tranId_4, usr1_certId_0, didChaincodeId, updateGrantOperateStatus, authStatus.toJSONString(), 0, "");
        postClient.postSignedTran(tran_4);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_4 = getTransactionResult(tranId_4);
        Peer.ActionResult actionResult_4 = tranResult_4.getErr();
        Assertions.assertEquals(0, actionResult_4.getCode(), "没有错误，启用授权成功");
    }


    @Test
    @Order(9)
    @DisplayName("禁用操作-具有传递性")
    void testUpdateOperateStatus_3() throws InterruptedException, InvalidProtocolBufferException {
        // step1: usr1禁用操作
        String tranId_1 = UUID.randomUUID().toString();
        JSONObject operStatus = new JSONObject();
        operStatus.fluentPut("opId", DigestUtils.sha256Hex("identity-net.CredenceTPL.creProof2")).fluentPut("state", false);
        Peer.Transaction tran_1 = usr1_tranCreator_0.createInvokeTran(tranId_1, usr1_certId_0, didChaincodeId, updateOperateStatus, operStatus.toJSONString(), 0, "");
        postClient.postSignedTran(tran_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_1 = getTransactionResult(tranId_1);
        Peer.ActionResult actionResult_1 = tranResult_1.getErr();
        Assertions.assertEquals(0, actionResult_1.getCode(), "没有错误，禁用操作成功");

        // step2: usr0和usr2不能调用, 操作禁用具有传递性，usr0不能调用CredenceProofTPL.creProof2的操作
        String tranId_2 = UUID.randomUUID().toString();
        Peer.ChaincodeId credenceTPLId = Peer.ChaincodeId.newBuilder().setChaincodeName("CredenceTPL").setVersion(2).build();
        Peer.Transaction tran_2 = usr0_tranCreator_0.createInvokeTran(tranId_2, usr0_certId_0, credenceTPLId,
                "creProof2", "{\"uuid\" : \"121000005l35120456\",\"data\" : \"{\\\"data1\\\": \\\"xyb002\\\",\\\"data2\\\": \\\"xyb003\\\"}\"}", 0, "");
        postClient.postSignedTran(tran_2);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_2 = getTransactionResult(tranId_2);
        Peer.ActionResult actionResult_2 = tranResult_2.getErr();
        Assertions.assertEquals(101, actionResult_2.getCode(), "错误码为101");
        assertThat(actionResult_2.getReason()).isEqualTo("操作已经失效");

        // step2: usr2不能调用, 禁用操作具有传递性
        String tranId_3 = UUID.randomUUID().toString();
        Peer.ChaincodeId credenceTPLId_1 = Peer.ChaincodeId.newBuilder().setChaincodeName("CredenceTPL").setVersion(1).build();
        Peer.Transaction tran_3 = usr2_tranCreator_0.createInvokeTran(tranId_3, usr2_certId_0, credenceTPLId_1,
                "creProof2", "{\"uuid\" : \"121000005l35120456\",\"data\" : \"{\\\"data1\\\": \\\"xyb002\\\",\\\"data2\\\": \\\"xyb003\\\"}\"}", 0, "");
        postClient.postSignedTran(tran_3);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_3 = getTransactionResult(tranId_3);
        Peer.ActionResult actionResult_3 = tranResult_3.getErr();
        Assertions.assertEquals(101, actionResult_3.getCode(), "错误码为101");
        assertThat(actionResult_3.getReason()).isEqualTo("操作已经失效");

        // 启用操作
        String tranId_4 = UUID.randomUUID().toString();
        operStatus.fluentPut("state", true);
        Peer.Transaction tran_4 = usr1_tranCreator_0.createInvokeTran(tranId_4, usr1_certId_0, didChaincodeId, updateOperateStatus, operStatus.toJSONString(), 0, "");
        postClient.postSignedTran(tran_4);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_4 = getTransactionResult(tranId_4);
        Peer.ActionResult actionResult_4 = tranResult_4.getErr();
        Assertions.assertEquals(0, actionResult_4.getCode(), "没有错误，禁用操作成功");

    }

    @Test
    @Order(10)
    @DisplayName("禁用授权-usr0禁用不存在的授权，禁用非自己的授权")
    void testUpdateGrantOperateStatus_4() throws InterruptedException, InvalidProtocolBufferException {
        // step1: usr0禁用不存在的授权
        JSONObject authStatus = new JSONObject();
        String authId = UUID.randomUUID().toString();
        authStatus.put("authId", authId);
        authStatus.put("state", false);
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = usr0_tranCreator_0.createInvokeTran(tranId, usr0_certId_0, didChaincodeId, updateGrantOperateStatus, authStatus.toJSONString(), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(102, actionResult.getCode(), "错误码为102");
        JSONObject errMsg = JSONObject.parseObject(actionResult.getReason());
        Assertions.assertEquals(15003, errMsg.getInteger("code"), String.format("authId为%s的Authorize不存在", authId));

        // step2: superAdmin禁用任何人的授权
        authStatus.put("authId", deployCreProof2AuthId_1);
        authStatus.put("state", false);
        String tranId_1 = UUID.randomUUID().toString();
        Peer.Transaction tran_1 = superCreator.createInvokeTran(tranId_1, superCertId, didChaincodeId, updateGrantOperateStatus, authStatus.toJSONString(), 0, "");
        postClient.postSignedTran(tran_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_1 = getTransactionResult(tranId_1);
        Peer.ActionResult actionResult_1 = tranResult_1.getErr();
        Assertions.assertEquals(0, actionResult_1.getCode(), "没有错误，禁用授权成功");

    }

    @Test
    @Order(10)
    @DisplayName("绑定授权-usr0禁用不存在的授权，禁用非自己的授权")
    void testBindCertToAuthorize_1() throws InterruptedException, InvalidProtocolBufferException {

        // step1: 绑定授权到的证书在自己名下，不可以绑定到非自己名下的证书
        Peer.BindCertToAuthorize bca = Peer.BindCertToAuthorize.newBuilder()
                .setAuthorizeId(funcCreProof2AuthId)
                .setGranted(usr2_certId_0)
                .setVersion("1.0")
                .build();
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = usr0_tranCreator_0.createInvokeTran(tranId, usr0_certId_0, didChaincodeId, bindCertToAuthorize, JsonFormat.printer().print(bca), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(102, actionResult.getCode(), "错误码为102");
        JSONObject errMsg = JSONObject.parseObject(actionResult.getReason());
        Assertions.assertEquals(15006, errMsg.getInteger("code"), "不能绑定Authorize到非签名交易提交者的证书上");
    }

    @Test
    @Order(11)
    @DisplayName("绑定授权-要绑定的授权，交易提交者名下没有")
    void testBindCertToAuthorize_2() throws InterruptedException, InvalidProtocolBufferException {

        // step2: 要绑定的授权，交易提交者名下没有
        Peer.BindCertToAuthorize bca_1 = Peer.BindCertToAuthorize.newBuilder()
                .setAuthorizeId(UUID.randomUUID().toString())
                .setGranted(usr0_certId_0)
                .setVersion("1.0")
                .build();
        String tranId_1 = UUID.randomUUID().toString();
        Peer.Transaction tran_1 = usr0_tranCreator_0.createInvokeTran(tranId_1, usr0_certId_0, didChaincodeId, bindCertToAuthorize, JsonFormat.printer().print(bca_1), 0, "");
        postClient.postSignedTran(tran_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_1 = getTransactionResult(tranId_1);
        Peer.ActionResult actionResult_1 = tranResult_1.getErr();
        Assertions.assertEquals(102, actionResult_1.getCode(), "错误码为102");
        JSONObject errMsg_1 = JSONObject.parseObject(actionResult_1.getReason());
        Assertions.assertEquals(15003, errMsg_1.getInteger("code"), String.format("authId为%s的Authorize不存在", bca_1.getAuthorizeId()));
    }


    @Test
    @Order(12)
    @DisplayName("绑定授权-要绑定的授权，已经被禁用")
    void testBindCertToAuthorize_3() throws InterruptedException, InvalidProtocolBufferException {

        // step3: 要绑定的授权，已经被禁用，这里需要讨论
        Peer.BindCertToAuthorize bca_2 = Peer.BindCertToAuthorize.newBuilder()
                .setAuthorizeId(deployCreProof2AuthId_1)
                .setGranted(usr1_certId_0)
                .setVersion("1.0")
                .build();
        String tranId_2 = UUID.randomUUID().toString();
        Peer.Transaction tran_2 = usr1_tranCreator_0.createInvokeTran(tranId_2, usr1_certId_0, didChaincodeId, bindCertToAuthorize, JsonFormat.printer().print(bca_2), 0, "");
        postClient.postSignedTran(tran_2);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_2 = getTransactionResult(tranId_2);
        Peer.ActionResult actionResult_2 = tranResult_2.getErr();
        Assertions.assertEquals(102, actionResult_2.getCode(), "错误码为102");
        JSONObject errMsg_2 = JSONObject.parseObject(actionResult_2.getReason());
        Assertions.assertEquals(15004, errMsg_2.getInteger("code"), String.format("authId为%s的Authorize无效", bca_2.getAuthorizeId()));

        // 启用授权
        JSONObject authStatus = new JSONObject();
        authStatus.put("authId", deployCreProof2AuthId_1);
        authStatus.put("state", true);
        String tranId_3 = UUID.randomUUID().toString();
        Peer.Transaction tran_3 = usr0_tranCreator_0.createInvokeTran(tranId_3, usr0_certId_0, didChaincodeId, updateGrantOperateStatus, authStatus.toJSONString(), 0, "");
        postClient.postSignedTran(tran_3);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_3 = getTransactionResult(tranId_3);
        Peer.ActionResult actionResult_3 = tranResult_3.getErr();
        Assertions.assertEquals(0, actionResult_3.getCode(), "没有错误，启用授权成功");
    }


    @Test
    @Order(13)
    @DisplayName("绑定授权-要绑定的授权，证书不存在")
    void testBindCertToAuthorize_4() throws InterruptedException, InvalidProtocolBufferException {

        // step4: 要绑定的证书不存在
        Peer.CertId usr0_certId_2 = Peer.CertId.newBuilder()
                .setCreditCode(user0_creditCode)
                .setCertName("not-exists")
                .build();
        Peer.BindCertToAuthorize bca_3 = Peer.BindCertToAuthorize.newBuilder()
                .setAuthorizeId(funcCreProof2AuthId)
                .setGranted(usr0_certId_2)
                .setVersion("1.0")
                .build();
        String tranId_4 = UUID.randomUUID().toString();
        Peer.Transaction tran_4 = usr0_tranCreator_0.createInvokeTran(tranId_4, usr0_certId_0, didChaincodeId, bindCertToAuthorize, JsonFormat.printer().print(bca_3), 0, "");
        postClient.postSignedTran(tran_4);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_4 = getTransactionResult(tranId_4);
        Peer.ActionResult actionResult_4 = tranResult_4.getErr();
        Assertions.assertEquals(102, actionResult_4.getCode(), "错误码为102");
        JSONObject errMsg_4 = JSONObject.parseObject(actionResult_4.getReason());
        Assertions.assertEquals(15007, errMsg_4.getInteger("code"), "要绑定的证书不存在");

    }

    @Test
    @Order(14)
    @DisplayName("绑定授权-要绑定的授权，证书无效")
    void testBindCertToAuthorize_5() throws InterruptedException, InvalidProtocolBufferException {
        // step5: 要绑定的证书无效，这里需要讨论
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

        // 尝试绑定证书
        Peer.BindCertToAuthorize bca_3 = Peer.BindCertToAuthorize.newBuilder()
                .setAuthorizeId(funcCreProof2AuthId)
                .setGranted(usr0_certId_1)
                .setVersion("1.0")
                .build();
        String tranId_4 = UUID.randomUUID().toString();
        Peer.Transaction tran_4 = usr0_tranCreator_0.createInvokeTran(tranId_4, usr0_certId_0, didChaincodeId, bindCertToAuthorize, JsonFormat.printer().print(bca_3), 0, "");
        postClient.postSignedTran(tran_4);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_4 = getTransactionResult(tranId_4);
        Peer.ActionResult actionResult_4 = tranResult_4.getErr();
        Assertions.assertEquals(102, actionResult_4.getCode(), "错误码为102");
        JSONObject errMsg_4 = JSONObject.parseObject(actionResult_4.getReason());
        Assertions.assertEquals(15008, errMsg_4.getInteger("code"), "要绑定的证书无效");

        // 恢复身份证书状态
        String tranId_2 = UUID.randomUUID().toString();
        // 修改身份证书状态
        certStatus.fluentPut("creditCode", user0_creditCode).fluentPut("certName", user0_cert_1).fluentPut("state", true);
        Peer.Transaction tran_2 = usr0_tranCreator_0.createInvokeTran(tranId_2, usr0_certId_0, didChaincodeId, updateCertificateStatus, certStatus.toJSONString(), 0, "");
        postClient.postSignedTran(tran_2);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_2 = getTransactionResult(tranId_2);
        Assertions.assertEquals(0, tranResult_2.getErr().getCode(), "没有错误，修改成功");

        // 再次尝试绑定证书
        String tranId_3 = UUID.randomUUID().toString();
        Peer.Transaction tran_3 = usr0_tranCreator_1.createInvokeTran(tranId_3, usr0_certId_1, didChaincodeId, bindCertToAuthorize, JsonFormat.printer().print(bca_3), 0, "");
        postClient.postSignedTran(tran_3);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_3 = getTransactionResult(tranId_3);
        Peer.ActionResult actionResult_3 = tranResult_3.getErr();
        Assertions.assertEquals(0, tranResult_3.getErr().getCode(), "没有错误，绑定成功");
    }


    @Test
    @Order(15)
    @DisplayName("绑定授权-绑定了的证书可以调用，没有绑定授权的证书不能调用了")
    void testBindCertToAuthorize_6() throws InterruptedException, InvalidProtocolBufferException {

        Peer.ChaincodeId credenceTPLId = Peer.ChaincodeId.newBuilder().setChaincodeName("CredenceTPL").setVersion(2).build();

        // step6: 使用绑定授权的证书，可以调用
        String tranId_1 = UUID.randomUUID().toString();
        Peer.Transaction tran_1 = usr0_tranCreator_1.createInvokeTran(tranId_1, usr0_certId_1, credenceTPLId,
                "creProof2", "{\"uuid\" : \"121000005l35120456\",\"data\" : \"{\\\"data1\\\": \\\"xyb002\\\",\\\"data2\\\": \\\"xyb003\\\"}\"}", 0, "");
        postClient.postSignedTran(tran_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_1 = getTransactionResult(tranId_1);
        Peer.ActionResult actionResult_1 = tranResult_1.getErr();
        Assertions.assertEquals(0, actionResult_1.getCode(), "usr0-1存证成功");

        // step7: 使用没绑定授权的证书，不能调用
        String tranId_2 = UUID.randomUUID().toString();
        Peer.Transaction tran_2 = usr0_tranCreator_0.createInvokeTran(tranId_2, usr0_certId_0, credenceTPLId,
                "creProof2", "{\"uuid\" : \"121000005l35120456\",\"data\" : \"{\\\"data1\\\": \\\"xyb002\\\",\\\"data2\\\": \\\"xyb003\\\"}\"}", 0, "");
        postClient.postSignedTran(tran_2);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_2 = getTransactionResult(tranId_2);
        Peer.ActionResult actionResult_2 = tranResult_2.getErr();
        Assertions.assertEquals(101, actionResult_2.getCode(), "错误码为101");
        assertThat(actionResult_2.getReason()).isEqualTo("授权已经失效");

        // step8: 使用绑定授权的证书，可以调用
        Peer.BindCertToAuthorize bca = Peer.BindCertToAuthorize.newBuilder()
                .setAuthorizeId(funcCreProof2AuthId)
                .setGranted(usr0_certId_0)
                .setVersion("1.0")
                .build();
        String tranId_3 = UUID.randomUUID().toString();
        Peer.Transaction tran_3 = usr0_tranCreator_1.createInvokeTran(tranId_3, usr0_certId_1, didChaincodeId, bindCertToAuthorize, JsonFormat.printer().print(bca), 0, "");
        postClient.postSignedTran(tran_3);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_3 = getTransactionResult(tranId_3);
        Peer.ActionResult actionResult_3 = tranResult_3.getErr();
        Assertions.assertEquals(0, tranResult_3.getErr().getCode(), "没有错误，绑定成功");

        // step9: 使用没绑定授权的证书，不能调用
        String tranId_4 = UUID.randomUUID().toString();
        Peer.Transaction tran_4 = usr0_tranCreator_0.createInvokeTran(tranId_4, usr0_certId_0, credenceTPLId,
                "creProof2", "{\"uuid\" : \"121000005l35120456\",\"data\" : \"{\\\"data1\\\": \\\"xyb002\\\",\\\"data2\\\": \\\"xyb003\\\"}\"}", 0, "");
        postClient.postSignedTran(tran_4);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_4 = getTransactionResult(tranId_4);
        Peer.ActionResult actionResult_4 = tranResult_4.getErr();
        Assertions.assertEquals(0, actionResult_4.getCode(), "usr0-0存证成功");

    }

}
