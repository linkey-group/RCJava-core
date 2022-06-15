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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.google.common.truth.Truth.assertThat;

/**
 * 本轮结束后
 * 1. usr0拥有的auth: signUpAllTypeCertificate, CredenceTPL.deploy; 拥有的oper: CredenceTPL.creProof
 * 2. usr1拥有的auth: signUpAllTypeCertificate, updateAllTypeCertificateStatus
 *
 * @author zyf
 */
@Tags({@Tag("注册Operate"), @Tag("修改Operate状态")})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OperOperationTest extends DidTest {

    public OperOperationTest() throws IOException {
    }

//    @Disabled
    @Test
    @DisplayName("注册Operate-注册service, onlySuperAdmin")
    @Order(1)
    void testSignUpOperate_1() throws InterruptedException, InvalidProtocolBufferException {
        // step1: node1注册service的Operate失败，只能是SuperAdmin才有权限
        long millis = System.currentTimeMillis();
        Peer.Operate operate = Peer.Operate.newBuilder()
                .setOpId(DigestUtils.sha256Hex(did_network_id + "service_" + UUID.randomUUID()))
                .setDescription("测试用的service")
                .setRegister(super_creditCode)
                .setIsPublish(false)
                .setOperateType(Peer.Operate.OperateType.OPERATE_SERVICE)
                .addOperateServiceName(did_network_id + "service_" + UUID.randomUUID())
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
                .setOpValid(true)
                .setVersion("1.0")
                .build();
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = node1Creator.createInvokeTran(tranId, node1CertId, didChaincodeId, signUpOperate, JsonFormat.printer().print(operate), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(102, actionResult.getCode(), "错误码为102");
        JSONObject errMsg = JSONObject.parseObject(actionResult.getReason());
        Assertions.assertEquals(14005, errMsg.getInteger("code"), "非管理员，不具有管理Service的权限");
        Assertions.assertEquals("非管理员，不具有管理Service的权限", errMsg.getString("reason"));

        // step2: superAdmin成功
        String tranId_1 = UUID.randomUUID().toString();
        Peer.Transaction tran_1 = superCreator.createInvokeTran(tranId_1, superCertId, didChaincodeId, signUpOperate, JsonFormat.printer().print(operate), 0, "");
        postClient.postSignedTran(tran_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_1 = getTransactionResult(tranId_1);
        Peer.ActionResult actionResult_1 = tranResult_1.getErr();
        Assertions.assertEquals(0, actionResult_1.getCode(), "注册成功");

    }

//    @Disabled
    @Test
    @DisplayName("注册Operate-注册<.deploy>与<.setState>, onlySuperAdmin")
    @Order(2)
    void testSignUpOperate_2() throws InterruptedException, InvalidProtocolBufferException {
        // step1: usr0注册<.deploy>与<.setState>"的Operate失败，只能是SuperAdmin才有权限
        long millis = System.currentTimeMillis();
        Peer.Operate operate = Peer.Operate.newBuilder()
                .setOpId(DigestUtils.sha256Hex("identity-net:CredenceTPL.deploy"))
                .setDescription("发布合约操作")
                .setRegister(user1_creditCode_did)
                .setIsPublish(false)
                .setOperateType(Peer.Operate.OperateType.OPERATE_CONTRACT)
                // 貌似没必要？
                .addAllOperateServiceName(Arrays.asList("transaction.stream", "transaction.postTranByString", "transaction.postTranStream", "transaction.postTran"))
                .setAuthFullName("identity-net:TestProofTPL.deploy")
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
                .setOpValid(true)
                .setVersion("1.0")
                .build();
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = usr0_tranCreator_1.createInvokeTran(tranId, usr0_certId_1, didChaincodeId, signUpOperate, JsonFormat.printer().print(operate), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(102, actionResult.getCode(), "错误码为102");
        JSONObject errMsg = JSONObject.parseObject(actionResult.getReason());
        Assertions.assertEquals(14005, errMsg.getInteger("code"), "非管理员，不能注册管理员相关的操作，如：setState与deploy");
        Truth.assertThat(errMsg.getString("reason")).isEqualTo("非管理员，不能注册管理员相关的操作，如：setState与deploy");

        // step2: superAdmin成功

    }

//    @Disabled
    @Test
    @DisplayName("注册Operate-注册合约")
    @Order(3)
    void testDeployContract() throws IOException, InterruptedException {
        // step1: usr0部署合约A失败, 操作不存在
        // CredenceTPL.scala 是事先编写好的合约文件
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
        Peer.Transaction signedDeployTran = usr0_tranCreator_0.createDeployTran(deployTran);
        postClient.postSignedTran(signedDeployTran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(signedDeployTran.getId());
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(101, actionResult.getCode(), "错误码为101");
        assertThat(actionResult.getReason()).isIn(Arrays.asList("操作不存在", "没有找到授权的操作"));

        // step2: superAdmin授权给usr0部署合约A的权限, superAdmin不具有该操作, 因此授权失败
        long millis = System.currentTimeMillis();
        Peer.Authorize authorize = Peer.Authorize.newBuilder()
                .setId(did_network_id + UUID.randomUUID().toString())
                .setGrant(super_creditCode)
                .addGranted(user0_creditCode_did)
                .addOpId(DigestUtils.sha256Hex(did_network_id + "TestProof.deploy"))
                .setIsTransfer(Peer.Authorize.TransferType.TRANSFER_REPEATEDLY)
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
                .setAuthorizeValid(true)
                .setVersion("1.0")
                .build();
        String tranId_1 = UUID.randomUUID().toString();
        Peer.Transaction tran_1 = superCreator.createInvokeTran(tranId_1, superCertId, didChaincodeId, grantOperate,
                JSONObject.toJSONString(Collections.singletonList(JsonFormat.printer().print(authorize))), 0, "");
        postClient.postSignedTran(tran_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_1 = getTransactionResult(tranId_1);
        Peer.ActionResult actionResult_1 = tranResult_1.getErr();
        Assertions.assertEquals(102, actionResult_1.getCode(), "错误码为102");
        JSONObject errMsg_1 = JSONObject.parseObject(actionResult_1.getReason());
        Assertions.assertEquals(15001, errMsg_1.getInteger("code"), "部分操作不存在或者无效");

        // step3: superAdmin注册部署合约A的操作
        long millis_1 = System.currentTimeMillis();
        Peer.Operate operate = Peer.Operate.newBuilder()
                .setOpId(DigestUtils.sha256Hex("identity-net:CredenceTPL.deploy"))
                .setDescription("发布合约-CredenceTPL")
                .setRegister(super_creditCode)
                .setIsPublish(false)
                .setOperateType(Peer.Operate.OperateType.OPERATE_CONTRACT)
                // 貌似没必要？
                .addAllOperateServiceName(Arrays.asList("transaction.stream", "transaction.postTranByString", "transaction.postTranStream", "transaction.postTran"))
                .setAuthFullName("identity-net:CredenceTPL.deploy")
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis_1 / 1000).setNanos((int) ((millis_1 % 1000) * 1000000)).build())
                .setOpValid(true)
                .setVersion("1.0")
                .build();
        String tranId_2 = UUID.randomUUID().toString();
        Peer.Transaction tran_2 = superCreator.createInvokeTran(tranId_2, superCertId, didChaincodeId, signUpOperate, JsonFormat.printer().print(operate), 0, "");
        postClient.postSignedTran(tran_2);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_2 = getTransactionResult(tranId_2);
        Peer.ActionResult actionResult_2 = tranResult_2.getErr();
        Assertions.assertEquals(0, actionResult_2.getCode(), "没有错误，操作注册成功");

        // step3: usr0部署合约B失败
        // step3: node1授权usr0部署任何合约的权限

        // step4: superAdmin授权给usr0部署合约A的权限
        String tranId_3 = UUID.randomUUID().toString();
        Peer.Authorize authorize_1 = authorize.toBuilder().clearOpId().addOpId(DigestUtils.sha256Hex("identity-net:CredenceTPL.deploy")).build();
        Peer.Transaction tran_3 = superCreator.createInvokeTran(tranId_3, superCertId, didChaincodeId, grantOperate,
                JSONObject.toJSONString(Collections.singletonList(JsonFormat.printer().print(authorize_1))), 0, "");
        postClient.postSignedTran(tran_3);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_3 = getTransactionResult(tranId_3);
        Peer.ActionResult actionResult_3 = tranResult_3.getErr();
        Assertions.assertEquals(0, actionResult_3.getCode(), "没有错误，授权成功");

        // step5: usr0部署合约A成功
        DeployTran deployTran_1 = deployTran.toBuilder().setTxid(UUID.randomUUID().toString()).build();
        Peer.Transaction signedDeployTran_1 = usr0_tranCreator_0.createDeployTran(deployTran_1);
        postClient.postSignedTran(signedDeployTran_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_4 = getTransactionResult(signedDeployTran_1.getId());
        Peer.ActionResult actionResult_4 = tranResult_4.getErr();
        Assertions.assertEquals(0, actionResult_4.getCode(), "没有错误，合约部署成功");

    }

    @Test
    @DisplayName("注册Operate-注册合约的某个方法")
    @Order(4)
    void testSignUpOperate_4() throws InterruptedException, InvalidProtocolBufferException {
        // step1: usr0注册不存在的合约的某个方法的Operate失败, 合约部署者不存在
        long millis = System.currentTimeMillis();
        Peer.Operate operate = Peer.Operate.newBuilder()
                .setOpId(DigestUtils.sha256Hex("identity-net:CredenceTPL-2.creProof"))
                .setDescription("测试注册合约某个方法")
                .setRegister(user0_creditCode_did)
                .setIsPublish(true)
                .setOperateType(Peer.Operate.OperateType.OPERATE_CONTRACT)
                // 貌似没必要？
                .addAllOperateServiceName(Arrays.asList("transaction.stream", "transaction.postTranByString", "transaction.postTranStream", "transaction.postTran"))
                .setAuthFullName("identity-net:CredenceTPL-2.creProof")
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
                .setOpValid(true)
                .setVersion("1.0")
                .build();
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = usr0_tranCreator_0.createInvokeTran(tranId, usr0_certId_0, didChaincodeId, signUpOperate, JsonFormat.printer().print(operate), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(102, actionResult.getCode(), "错误码为102");
        JSONObject errMsg = JSONObject.parseObject(actionResult.getReason());
        Assertions.assertEquals(14003, errMsg.getInteger("code"), "不具有该合约部署部署权限者，不能注册或禁用相应操作");

        // step2: usr1注册合约A的某个方法的Operate失败, 非合约部署者
        Peer.Operate operate_1 = operate.toBuilder()
                .setOpId(DigestUtils.sha256Hex("identity-net:CredenceTPL.creProof"))
                .setAuthFullName("identity-net:CredenceTPL.creProof")
                .setRegister(user1_creditCode_did)
                .build();
        String tranId_1 = UUID.randomUUID().toString();
        Peer.Transaction tran_1 = usr1_tranCreator_0.createInvokeTran(tranId_1, usr1_certId_0, didChaincodeId, signUpOperate, JsonFormat.printer().print(operate_1), 0, "");
        postClient.postSignedTran(tran_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_1 = getTransactionResult(tranId_1);
        Peer.ActionResult actionResult_1 = tranResult_1.getErr();
        Assertions.assertEquals(102, actionResult_1.getCode(), "错误码为102");
        JSONObject errMsg_1 = JSONObject.parseObject(actionResult_1.getReason());
        Assertions.assertEquals(14003, errMsg_1.getInteger("code"), "不具有该合约部署部署权限者，不能注册或禁用相应操作");
    }

    @Test
    @DisplayName("注册Operate-注册合约的某个方法, hash不一致")
    @Order(5)
    void testSignUpOperate_5() throws InterruptedException, InvalidProtocolBufferException {
        // step1: usr0注册合约A的某个方法的Operate失败, Hash不一致
        long millis = System.currentTimeMillis();
        Peer.Operate operate = Peer.Operate.newBuilder()
                .setOpId(DigestUtils.sha256Hex("identity-net:CredenceTPL.creProof-not-exists"))
                .setDescription("测试注册合约某个方法")
                .setRegister(user0_creditCode_did)
                .setIsPublish(true)
                .setOperateType(Peer.Operate.OperateType.OPERATE_CONTRACT)
                // 貌似没必要？
                .addAllOperateServiceName(Arrays.asList("transaction.stream", "transaction.postTranByString", "transaction.postTranStream", "transaction.postTran"))
                .setAuthFullName("identity-net:CredenceTPL.creProof")
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
                .setOpValid(true)
                .setVersion("1.0")
                .build();
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = usr0_tranCreator_0.createInvokeTran(tranId, usr0_certId_0, didChaincodeId, signUpOperate, JsonFormat.printer().print(operate), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(102, actionResult.getCode(), "错误码为102");
        JSONObject errMsg = JSONObject.parseObject(actionResult.getReason());
        Assertions.assertEquals(14007, errMsg.getInteger("code"), "Operate中opId字段与计算得到的Hash不相等");

    }

    @Test
    @DisplayName("注册Operate-注册合约的某个方法, operate中的register为usr1")
    @Order(6)
    void testSignUpOperate_6() throws InterruptedException, InvalidProtocolBufferException {
        // step1: usr0只能为自己注册的合约的某个方法的Operate失败, operate中的register为usr1
        long millis = System.currentTimeMillis();
        Peer.Operate operate = Peer.Operate.newBuilder()
                .setOpId(DigestUtils.sha256Hex("identity-net:CredenceTPL.creProof"))
                .setDescription("测试注册合约某个方法")
                .setRegister(user1_creditCode_did)
                .setIsPublish(true)
                .setOperateType(Peer.Operate.OperateType.OPERATE_CONTRACT)
                // 貌似没必要？
                .addAllOperateServiceName(Arrays.asList("transaction.stream", "transaction.postTranByString", "transaction.postTranStream", "transaction.postTran"))
                .setAuthFullName("identity-net:CredenceTPL.creProof")
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
                .setOpValid(true)
                .setVersion("1.0")
                .build();
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = usr0_tranCreator_0.createInvokeTran(tranId, usr0_certId_0, didChaincodeId, signUpOperate, JsonFormat.printer().print(operate), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(102, actionResult.getCode(), "错误码为102");
        JSONObject errMsg = JSONObject.parseObject(actionResult.getReason());
        Assertions.assertEquals(14004, errMsg.getInteger("code"), "register(操作注册者)非交易提交者");
    }

    @Test
    @DisplayName("注册Operate-注册合约的某个方法, 只能注册一次，成功后, 不能重复注册")
    @Order(7)
    void testSignUpOperate_7() throws InterruptedException, InvalidProtocolBufferException {
        // step1: usr0注册合约A的某个方法的Operate成功
        long millis = System.currentTimeMillis();
        Peer.Operate operate = Peer.Operate.newBuilder()
                .setOpId(DigestUtils.sha256Hex("identity-net:CredenceTPL.creProof"))
                .setDescription("测试注册合约某个方法")
                .setRegister(user0_creditCode_did)
                .setIsPublish(false)
                .setOperateType(Peer.Operate.OperateType.OPERATE_CONTRACT)
                // 貌似没必要？
                .addAllOperateServiceName(Arrays.asList("transaction.stream", "transaction.postTranByString", "transaction.postTranStream", "transaction.postTran"))
                .setAuthFullName("identity-net:CredenceTPL.creProof")
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
                .setOpValid(true)
                .setVersion("1.0")
                .build();
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = usr0_tranCreator_0.createInvokeTran(tranId, usr0_certId_0, didChaincodeId, signUpOperate, JsonFormat.printer().print(operate), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(0, actionResult.getCode(), "没有错误，操作注册成功");

        // step2: usr0重复注册合约A的某个方法的Operate失败
        String tranId_1 = UUID.randomUUID().toString();
        Peer.Transaction tran_1 = usr0_tranCreator_0.createInvokeTran(tranId_1, usr0_certId_0, didChaincodeId, signUpOperate, JsonFormat.printer().print(operate), 0, "");
        postClient.postSignedTran(tran_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_1 = getTransactionResult(tranId_1);
        Peer.ActionResult actionResult_1 = tranResult_1.getErr();
        Assertions.assertEquals(102, actionResult_1.getCode(), "错误码为102");
        JSONObject errMsg_1 = JSONObject.parseObject(actionResult_1.getReason());
        Assertions.assertEquals(14001, errMsg_1.getInteger("code"), "operate已存在");
    }


    @Test
    @DisplayName("提交存证交易")
    @Order(8)
    String testCredenceProof() throws InterruptedException {
        // step1: usr0修改不存在的operate的状态失败
        Peer.ChaincodeId credenceTPLId = Peer.ChaincodeId.newBuilder().setChaincodeName("CredenceTPL").setVersion(1).build();
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = usr0_tranCreator_0.createInvokeTran(tranId, usr0_certId_0, credenceTPLId,
                "creProof", "{\"uuid\" : \"121000005l35120456\",\"data\" : \"{\\\"data1\\\": \\\"xyb002\\\",\\\"data2\\\": \\\"xyb003\\\"}\"}", 0, "");
        String tranHex = Hex.encodeHexString(tran.toByteArray());
        postClient.postSignedTran(tranHex);
        TimeUnit.SECONDS.sleep(2);
        return tranId;

    }


    @Test
    @DisplayName("修改Operate状态-要修改的operate不存在")
    @Order(9)
    void testUpdateOperateStatus_9() throws InterruptedException {
        // step1: usr0修改不存在的operate的状态失败
        String tranId = UUID.randomUUID().toString();
        JSONObject operStatus = new JSONObject();
        operStatus.fluentPut("opId", DigestUtils.sha256Hex("not-exists")).fluentPut("state", false);
        Peer.Transaction tran = usr0_tranCreator_0.createInvokeTran(tranId, usr0_certId_0, didChaincodeId, updateOperateStatus, operStatus.toJSONString(), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(102, actionResult.getCode(), "错误码为102");
        JSONObject errMsg = JSONObject.parseObject(actionResult.getReason());
        Assertions.assertEquals(14002, errMsg.getInteger("code"), "operate不存在");
        Assertions.assertEquals("operate不存在", errMsg.getString("reason"));

    }

    @Test
    @DisplayName("修改Operate状态-只能修改自己的operate")
    @Order(10)
    void testUpdateOperateStatus_10() throws InterruptedException {
        // step1: usr1修改usr0的operate的状态失败
        String tranId = UUID.randomUUID().toString();
        JSONObject operStatus = new JSONObject();
        operStatus.fluentPut("opId", DigestUtils.sha256Hex("identity-net:CredenceTPL.creProof")).fluentPut("state", false);
        Peer.Transaction tran = usr1_tranCreator_0.createInvokeTran(tranId, usr1_certId_0, didChaincodeId, updateOperateStatus, operStatus.toJSONString(), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult = getTransactionResult(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(102, actionResult.getCode(), "错误码为102");
        JSONObject errMsg = JSONObject.parseObject(actionResult.getReason());
        Assertions.assertEquals(14004, errMsg.getInteger("code"), "register(操作注册者)非交易提交者，只能操作注册者修改操作状态");
        Assertions.assertEquals("register(操作注册者)非交易提交者", errMsg.getString("reason"));

    }

    @Test
    @DisplayName("修改Operate状态-修改自己的operate成功")
    @Order(11)
    void testUpdateOperateStatus_11() throws InterruptedException {
        // step1: usr0修改usr0的operate的状态成功
        String tranId_1 = UUID.randomUUID().toString();
        JSONObject operStatus = new JSONObject();
        operStatus.fluentPut("opId", DigestUtils.sha256Hex("identity-net:CredenceTPL.creProof")).fluentPut("state", false);
        Peer.Transaction tran_1 = usr0_tranCreator_0.createInvokeTran(tranId_1, usr0_certId_0, didChaincodeId, updateOperateStatus, operStatus.toJSONString(), 0, "");
        postClient.postSignedTran(tran_1);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_1 = getTransactionResult(tranId_1);
        Peer.ActionResult actionResult_1 = tranResult_1.getErr();
        Assertions.assertEquals(0, actionResult_1.getCode(), "没有错误，提交成功");

        // step2: 提交交易测试
        String resId = testCredenceProof();
        Assertions.assertEquals("操作已经失效", getTransactionResult(resId).getErr().getReason());
    }

    @Test
    @DisplayName("修改Operate状态-superAdmin可以修改usr0的operate状态")
    @Order(12)
    void testUpdateOperateStatus_12() throws InterruptedException {
        // step1: superAdmin修改usr0的operate的状态
        String tranId_2 = UUID.randomUUID().toString();
        JSONObject operStatus = new JSONObject();
        operStatus.fluentPut("opId", DigestUtils.sha256Hex("identity-net:CredenceTPL.creProof")).fluentPut("state", true);
        Peer.Transaction tran_2 = superCreator.createInvokeTran(tranId_2, superCertId, didChaincodeId, updateOperateStatus, operStatus.toJSONString(), 0, "");
        postClient.postSignedTran(tran_2);
        TimeUnit.SECONDS.sleep(2);
        Peer.TransactionResult tranResult_2 = getTransactionResult(tranId_2);
        Peer.ActionResult actionResult_2 = tranResult_2.getErr();
        Assertions.assertEquals(0, actionResult_2.getCode(), "没有错误，提交成功");

        // step4: 提交交易测试
        String resId_1 = testCredenceProof();
        Assertions.assertEquals(0, getTransactionResult(resId_1).getErr().getCode(), "没有错误，提交成功");
    }


}
