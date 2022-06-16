package com.rcjava.multi_chain;

import com.alibaba.fastjson2.JSON;
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
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author zyf
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ManageNodeCertTest extends DidTest {

    TranPostClient postClient = new TranPostClient("localhost:9081");
    ChainInfoClient infoClient = new ChainInfoClient("localhost:9081");

    TranPostClient postCredenceClient = new TranPostClient("localhost:9086");
    ChainInfoClient infoCredenceClient = new ChainInfoClient("localhost:9086");

    static String node1Name = "121000005l35120456.node1";
    static String node2Name = "12110107bi45jh675g.node2";
    static String node3Name = "122000002n00123567.node3";
    static String node4Name = "921000005k36123789.node4";
    static String node5Name = "921000006e0012v696.node5";

    static String node6Name = "330597659476689954.node6";
    static String node7Name = "044934755127708189.node7";
    static String node8Name = "201353514191149590.node8";
    static String node9Name = "734747416095474396.node9";
    static String node10Name = "710341838996249513.node10";


    Peer.ChaincodeId manageNodeCertId = Peer.ChaincodeId.newBuilder().setChaincodeName("ManageNodeCert").setVersion(1).build();

    public ManageNodeCertTest() throws IOException {
    }

    @BeforeAll
    void init_1() {
        super_pri = CertUtil.genX509CertPrivateKey(new File("jks/multi_chain/951002007l78123233.super_admin.jks"),
                "super_admin", "951002007l78123233.super_admin").getPrivateKey();
        System.out.println("jks/multi_chain/951002007l78123233.super_admin.jks");
        superCreator = TranCreator.newBuilder().setPrivateKey(super_pri).setSignAlgorithm("SHA256withECDSA").build();
    }

    @Test
    @DisplayName("SuperAdmin部署节点证书管理合约")
    @Order(1)
    void deployContractTest() throws InterruptedException, IOException {
        String tplString = FileUtils.readFileToString(new File("jks/did/tpl/ManageNodeCert.scala"), StandardCharsets.UTF_8);
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
                .setCertId(superCertId)
                .setChaincodeId(manageNodeCertId)
                .setChaincodeDeploy(chaincodeDeploy)
                .build();
        deployTran = deployTran.toBuilder().setTxid(UUID.randomUUID().toString()).build();
        Peer.Transaction signedDeployTran = superCreator.createDeployTran(deployTran);
        postCredenceClient.postSignedTran(signedDeployTran);
        TimeUnit.SECONDS.sleep(5);
        Peer.TransactionResult tranResult = infoCredenceClient.getTranResultByTranId(signedDeployTran.getId());
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(0, actionResult.getCode(), "没有错误，部署合约成功");

    }

    @Test
    @DisplayName("注册Operate-注册合约的某个方法---updateNodeCert")
    @Order(2)
    void testSignUpOperate() throws InterruptedException, InvalidProtocolBufferException {

        long millis = System.currentTimeMillis();
        Peer.Operate operate = Peer.Operate.newBuilder()
                .setOpId(DigestUtils.sha256Hex("credence-net.ManageNodeCert.updateNodeCert"))
                .setDescription("管理节点证书")
                .setRegister(super_creditCode)
                .setIsPublish(false)
                .setOperateType(Peer.Operate.OperateType.OPERATE_CONTRACT)
                .setAuthFullName("credence-net.ManageNodeCert.updateNodeCert")
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
                .setOpValid(true)
                .setVersion("1.0")
                .build();
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = superCreator.createInvokeTran(tranId, superCertId, didChaincodeId, signUpOperate, JsonFormat.printer().print(operate), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(5);
        Peer.TransactionResult tranResult = infoClient.getTranResultByTranId(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(0, actionResult.getCode(), "没有错误，操作注册成功");
    }

    @Test
    @DisplayName("提交交易，删除证书")
    @Order(3)
    void testDeleteCert() throws IOException, InterruptedException {
        String tranId = UUID.randomUUID().toString();
        HashMap<String, String> certMap = new HashMap<>();
        //certMap.put(node6Name,FileUtils.readFileToString(new File("jks/multi_chain/"+node6Name+".cer"),StandardCharsets.UTF_8));
        //certMap.put(node7Name,FileUtils.readFileToString(new File("jks/multi_chain/"+node7Name+".cer"),StandardCharsets.UTF_8));
        //certMap.put(node8Name,FileUtils.readFileToString(new File("jks/multi_chain/"+node8Name+".cer"),StandardCharsets.UTF_8));
        //certMap.put(node9Name,FileUtils.readFileToString(new File("jks/multi_chain/"+node9Name+".cer"),StandardCharsets.UTF_8));
        certMap.put(node10Name, "");

        Peer.Transaction tran = superCreator.createInvokeTran(tranId, superCertId, manageNodeCertId, "updateNodeCert", JSON.toJSONString(certMap), 0, "");
        postCredenceClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(5);
        Peer.TransactionResult tranResult = infoCredenceClient.getTranResultByTranId(tranId);
        System.out.println(tranResult);
    }


    @Test
    @DisplayName("提交交易，增加证书")
    @Order(4)
    void testAddCert() throws IOException, InterruptedException {
        String tranId = UUID.randomUUID().toString();
        HashMap<String, String> certMap = new HashMap<>();
        //certMap.put(node6Name,FileUtils.readFileToString(new File("jks/multi_chain/"+node6Name+".cer"),StandardCharsets.UTF_8));
        //certMap.put(node7Name,FileUtils.readFileToString(new File("jks/multi_chain/"+node7Name+".cer"),StandardCharsets.UTF_8));
        //certMap.put(node8Name,FileUtils.readFileToString(new File("jks/multi_chain/"+node8Name+".cer"),StandardCharsets.UTF_8));
        //certMap.put(node9Name,FileUtils.readFileToString(new File("jks/multi_chain/"+node9Name+".cer"),StandardCharsets.UTF_8));
        certMap.put(node10Name, FileUtils.readFileToString(new File("jks/multi_chain/" + node10Name + ".cer"), StandardCharsets.UTF_8));

        Peer.Transaction tran = superCreator.createInvokeTran(tranId, superCertId, manageNodeCertId, "updateNodeCert", JSON.toJSONString(certMap), 0, "");
        postCredenceClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(5);
        Peer.TransactionResult tranResult = infoCredenceClient.getTranResultByTranId(tranId);
        System.out.println(tranResult);
    }


    @Test
    @DisplayName("注册Operate-注册合约的某个方法---updateVoteList")
    @Order(5)
    void testSignUpOperate_1() throws InterruptedException, InvalidProtocolBufferException {

        long millis = System.currentTimeMillis();
        Peer.Operate operate = Peer.Operate.newBuilder()
                .setOpId(DigestUtils.sha256Hex("credence-net.ManageNodeCert.updateVoteList"))
                .setDescription("管理抽签列表")
                .setRegister(super_creditCode)
                .setIsPublish(false)
                .setOperateType(Peer.Operate.OperateType.OPERATE_CONTRACT)
                .setAuthFullName("credence-net.ManageNodeCert.updateVoteList")
                .setCreateTime(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
                .setOpValid(true)
                .setVersion("1.0")
                .build();
        String tranId = UUID.randomUUID().toString();
        Peer.Transaction tran = superCreator.createInvokeTran(tranId, superCertId, didChaincodeId, signUpOperate, JsonFormat.printer().print(operate), 0, "");
        postClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(5);
        Peer.TransactionResult tranResult = infoClient.getTranResultByTranId(tranId);
        Peer.ActionResult actionResult = tranResult.getErr();
        Assertions.assertEquals(0, actionResult.getCode(), "没有错误，操作注册成功");
    }

    @Test
    @DisplayName("提交交易，更新抽签列表")
    @Order(6)
    void testUpdateVoteList() throws InterruptedException {
        String tranId = UUID.randomUUID().toString();
        List<String> voteList = Arrays.asList("330597659476689954.node6", "044934755127708189.node7",
//                "201353514191149590.node8",
                "734747416095474396.node9", "710341838996249513.node10");
        Peer.Transaction tran = superCreator.createInvokeTran(tranId, superCertId, manageNodeCertId, "updateVoteList", JSON.toJSONString(voteList), 0, "");
        postCredenceClient.postSignedTran(tran);
        TimeUnit.SECONDS.sleep(5);
        Peer.TransactionResult tranResult = infoCredenceClient.getTranResultByTranId(tranId);
        System.out.println(tranResult);
    }
}
