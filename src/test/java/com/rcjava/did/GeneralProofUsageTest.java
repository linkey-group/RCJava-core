/**
 * Copyright  2024 Linkel Technology Co., Ltd, Beijing.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BA SIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rcjava.did;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import com.rcjava.client.ChainInfoClient;
import com.rcjava.client.TranPostClient;
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
import java.nio.file.Files;
import java.security.PrivateKey;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.rcjava.did.DidTest.*;

/**
 * 账户权限在身份链里管，具体业务合约在业务链部署和调用
 *
 * Created by 北京连琪科技有限公司.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GeneralProofUsageTest {

    // 身份链，指向一个身份链网络的节点
    TranPostClient postClient = new TranPostClient("localhost:9081");
    ChainInfoClient infoClient = new ChainInfoClient("localhost:9081");

    // 业务链，指向一个业务链网络的节点
    TranPostClient postCredenceClient = new TranPostClient("localhost:9086");
    ChainInfoClient infoCredenceClient = new ChainInfoClient("localhost:9086");

    // superAdmin
    TranCreator superCreator;
    Peer.CertId superCertId;

    // 新用户
    TranCreator usr0_tranCreator_0;
    Peer.CertId usr0_certId_0;

    // 身份管理合约名，版本号
    Peer.ChaincodeId didChaincodeId = Peer.ChaincodeId.newBuilder().setChaincodeName("RdidOperateAuthorizeTPL").setVersion(1).build();
    // 业务合约的合约名，版本号
    Peer.ChaincodeId generalProofTPLId = Peer.ChaincodeId.newBuilder().setChaincodeName("GeneralProofTPL").setVersion(1).build();


    @BeforeAll
    void init() {
        // 私钥
        PrivateKey super_pri = CertUtil.genX509CertPrivateKey(new File("jks/test/multi_chain/identity.951002007l78123233.super_admin.jks"),
                "super_admin", "951002007l78123233.super_admin").getPrivateKey();
        // 用来构造数字签名
        superCreator = TranCreator.newBuilder().setPrivateKey(super_pri).setSignAlgorithm("SHA256withECDSA").build();
        // 身份标识
        superCertId = Peer.CertId.newBuilder().setCreditCode("identity-net:951002007l78123233").setCertName("super_admin").build();

        PrivateKey usr0_pri = CertUtil.genX509CertPrivateKey(new File("jks/test/did/usr-0_0.jks"), "123", "usr_0_0").getPrivateKey();
        usr0_tranCreator_0 = TranCreator.newBuilder().setPrivateKey(usr0_pri).setSignAlgorithm("SHA256withECDSA").build();
        usr0_certId_0 = Peer.CertId.newBuilder().setCreditCode("credence-net:usr-0").setCertName("0").build();
    }

    @Test
    @DisplayName("SuperAdmin向身份链去注册用户usr-0，usr-0的账户以及证书都被注册了")
    @Order(1)
    void signUpSignerTest() throws InterruptedException, IOException {

        // 证书pem字符串
        String certPem = new String(Files.readAllBytes(new File("jks/test/did/usr-0_0.cer").toPath()));
        Peer.Certificate cert_proto = Peer.Certificate.newBuilder()
                // 新用户usr-0的证书
                .setCertificate(certPem)
                .setAlgType("sha256withecdsa")
                .setCertValid(true)
                .setCertType(Peer.Certificate.CertType.CERT_AUTHENTICATION)
                // usr-0可以随便定义
                .setId(Peer.CertId.newBuilder().setCreditCode("credence-net:usr-0").setCertName("0").build())
                .setCertHash(DigestUtils.sha256Hex(certPem.replaceAll("\r\n|\r|\n|\\s", "")))
                .build();

        //
        Peer.Signer usr0_signer = Peer.Signer.newBuilder()
                // name 可以随便设置
                .setName("usr-0")
                // 新用户usr-0的账户ID
                .setCreditCode("credence-net:usr-0") // 新注册用户   业务链前缀，credence-net:*****.usr-11
                .setMobile("18888888888")
                .addAllAuthenticationCerts(Collections.singletonList(cert_proto))
                .setSignerValid(true)
                .build();

        // super_admin向身份链提交交易，注册用户，以便在业务链使用
        String tranId_1 = UUID.randomUUID().toString();
        Peer.Transaction tran_1 = superCreator.createInvokeTran(tranId_1, superCertId, didChaincodeId, signUpSigner, JsonFormat.printer().print(usr0_signer), 0, "");
        postClient.postSignedTran(tran_1);
        TimeUnit.SECONDS.sleep(5);
        Peer.TransactionResult tranResult_1 = infoClient.getTranResultByTranId(tranId_1);
        Assertions.assertEquals(0, tranResult_1.getErr().getCode(), "没有错误，注册成功");
    }

    @Test
    @DisplayName("superAdmin注册Operate-可以部署合约GeneralProofTPL.deploy/*.deploy的操作")
    @Order(2)
    void signUpOperate() throws InterruptedException, IOException {

        // superAdmin注册部署合约credence-net:GeneralProofTPL.deploy的操作，向<身份链>提交
        long millis_1 = System.currentTimeMillis();
        Peer.Operate operate = Peer.Operate.newBuilder()
                // opId随意唯一，但是这里为了方便理解
                .setOpId(DigestUtils.sha256Hex("credence-net:*.deploy"))
                .setDescription("credence-net:*.deploy")
                .setRegister("identity-net:951002007l78123233")
                .setIsPublish(false)
                .setOperateType(Peer.Operate.OperateType.OPERATE_CONTRACT)
                .setAuthFullName("credence-net:*.deploy")
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis_1 / 1000).setNanos((int) ((millis_1 % 1000) * 1000000)).build())
                .setOpValid(true)
                .setVersion("1.0")
                .build();
        String tranId_2 = UUID.randomUUID().toString();
        // 调用身份管理合约
        Peer.Transaction tran_2 = superCreator.createInvokeTran(tranId_2, superCertId, didChaincodeId, signUpOperate, JsonFormat.printer().print(operate), 0, "");
        System.out.println(Hex.encodeHexString(tran_2.toByteArray()));
        postClient.postSignedTran(tran_2);
        TimeUnit.SECONDS.sleep(5);
        Peer.TransactionResult tranResult_2 = infoClient.getTranResultByTranId(tranId_2);
        Peer.ActionResult actionResult_2 = tranResult_2.getErr();
        Assertions.assertEquals(0, actionResult_2.getCode(), "没有错误，操作注册成功");

    }

    @Test
    @DisplayName("superAdmin授权给credence-net:usr-0可部署GeneralProofTPL的权限")
    @Order(3)
    void grantOperate() throws InterruptedException, IOException {

        // step4: superAdmin授权给usr0部署合约("credence-net:*.deploy")的权限，向<身份链>提交
        long millis = System.currentTimeMillis();
        Peer.Authorize authorize_1 = Peer.Authorize.newBuilder()
                .setId("credence-net:" + UUID.randomUUID())
                .setGrant("identity-net:951002007l78123233")  // 授权者，这里是superAdmin
                .addGranted("credence-net:usr-0")   // 被授权者 usr-0
                .addOpId(DigestUtils.sha256Hex("credence-net:*.deploy"))  // GeneralProofTPL，需要改一下
                .setIsTransfer(Peer.Authorize.TransferType.TRANSFER_REPEATEDLY) // 代表usr-0可以继续授权给别人
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
    }

    @Test
    @DisplayName("usr-0，具有了向业务链部署GeneralProofTPL的权限，因此向业务链部署合约")
    @Order(4)
    void testDeployContract() throws InterruptedException, IOException {
        // 向业务链提交，部署合约
        String tplString = FileUtils.readFileToString(new File("jks/test/tpl/GeneralProofTPL.scala"), StandardCharsets.UTF_8);
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
                .setChaincodeId(generalProofTPLId)
                .setChaincodeDeploy(chaincodeDeploy)
                .build();

        // step5: usr0部署合约A成功
        DeployTran deployTran_1 = deployTran.toBuilder().setTxid(UUID.randomUUID().toString()).build();
        Peer.Transaction signedDeployTran_1 = usr0_tranCreator_0.createDeployTran(deployTran_1);
        JSONObject postResult = postCredenceClient.postSignedTran(signedDeployTran_1);
        System.out.println(postResult);
        TimeUnit.SECONDS.sleep(10);
        Peer.TransactionResult tranResult_4 = infoCredenceClient.getTranResultByTranId(signedDeployTran_1.getId());
        Peer.ActionResult actionResult_4 = tranResult_4.getErr();
        Assertions.assertEquals(0, actionResult_4.getCode(), "没有错误，合约部署成功");
    }

    @Test
    @DisplayName("usr-0，作为合约部署者，注册合约里的Operate-注册合约的某个方法")
    @Order(5)
    void testSignUpOperate_1() throws InterruptedException, InvalidProtocolBufferException {

        // usr0注册合约A的某个方法的Operate成功，向身份链提交
        long millis = System.currentTimeMillis();
        Peer.Operate operate = Peer.Operate.newBuilder()
                // 合约的某个方法
                .setOpId(DigestUtils.sha256Hex("credence-net:GeneralProofTPL.proofLedgerData"))
                // 描述随意
                .setDescription("GeneralProofTPL.proofLedgerData")
                .setRegister("credence-net:usr-0")
                // 比如新注册了一个用户usr-1，如果是false的话，usr-1需要被授权才能调用creProof3，
                // 如果设置true，新注册的任何用户，都可以调用这个方法
                // TODO 是否是公开的，如果为false，则别的用户需要得到授权才可以调用，如果为true，则表示公开，任何人(注册用户)都可以调用
                .setIsPublish(true)
                .setOperateType(Peer.Operate.OperateType.OPERATE_CONTRACT)
                .setAuthFullName("credence-net:GeneralProofTPL.proofLedgerData") // 假设GeneralProofTPL 有多个版本
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
                .setOpValid(true)
                .setVersion("1.0")
                .build();
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = usr0_tranCreator_0.createInvokeTran(tranId, usr0_certId_0, didChaincodeId, signUpOperate, JsonFormat.printer().print(operate), 0, "");
        // 向身份链提交
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(5);
        Peer.TransactionResult tranResult = infoClient.getTranResultByTranId(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(0, actionResult.getCode(), "没有错误，操作注册成功");
    }

    @Test
    @DisplayName("调用业务链中刚部署的合约，存证数据")
    @Order(6)
    void testCredenceProof() throws InterruptedException {
        // step1: usr0提交存证交易，向业务链提交
        String tranId = UUID.randomUUID().toString();

        // 1. 构造单个对象
        JSONObject data = new JSONObject();
        data.put("uuid", UUID.randomUUID().toString());
        data.put("data", new JSONObject().fluentPut("id", "1").fluentPut("value", 100).toJSONString());

        // 2. 构造数组
        JSONArray array = new JSONArray();
        array.add(data);

        JSONObject data2 = new JSONObject();
        data2.put("uuid", UUID.randomUUID().toString());
        data2.put("data", new JSONObject().fluentPut("id", "2").fluentPut("value", 200).toJSONString());
        array.add(data2);

        // 3. 转换为字符串
        String jsonString = array.toString();
        System.out.println("JSON字符串: " + jsonString);


        Peer.Transaction tran = usr0_tranCreator_0.createInvokeTran(tranId, usr0_certId_0, generalProofTPLId, "proofLedgerData", array.toString(), 0, "");
        String tranHex = Hex.encodeHexString(tran.toByteArray());
        JSONObject postResult = postCredenceClient.postSignedTran(tranHex);
        System.out.println(postResult);
        TimeUnit.SECONDS.sleep(5);
        Peer.TransactionResult tranResult = infoCredenceClient.getTranResultByTranId(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(0, actionResult.getCode(), "存证成功");

    }

}
