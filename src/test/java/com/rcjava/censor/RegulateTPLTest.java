package com.rcjava.censor;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import com.rcjava.client.ChainInfoClient;
import com.rcjava.client.TranPostClient;
import com.rcjava.model.Conclusion;
import com.rcjava.protos.Peer;
import com.rcjava.tran.TranCreator;
import com.rcjava.util.CertUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.*;

/**
 * @description:
 * @author: daiyongbing
 * @date: 2023/4/6
 * @version: 1.0
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RegulateTPLTest {
    Logger logger = LoggerFactory.getLogger(RegulateTPLTest.class);
    private TranCreator superCreator;
    private final TranPostClient tranPostClient = new TranPostClient("localhost:9081");
    private final ChainInfoClient infoClient = new ChainInfoClient("localhost:9081");
    private Peer.CertId superCertId;

    private String illegal_tran_id = "";
    private Long illegal_block_height = 0L;

    private String illegal_block_hash;


    Peer.ChaincodeId regulateTPL = Peer.ChaincodeId.newBuilder().setChaincodeName("RegulateTPL").setVersion(1).build();
    Peer.ChaincodeId contractAssetsTPL = Peer.ChaincodeId.newBuilder().setChaincodeName("ContractAssetsTPL").setVersion(1).build();
    Peer.ChaincodeId didChaincodeId = Peer.ChaincodeId.newBuilder().setChaincodeName("RdidOperateAuthorizeTPL").setVersion(1).build();
    private final String network = "identity-net:";
    private final String super_credit = network + "951002007l78123233";

    @BeforeAll
    void init() {
        PrivateKey privateKey = CertUtil.genX509CertPrivateKey(new File("jks/jdk13/951002007l78123233.super_admin.jks"),
            "super_admin", "951002007l78123233.super_admin").getPrivateKey();
        superCreator = TranCreator.newBuilder().setPrivateKey(privateKey).setSignAlgorithm("SHA256withECDSA").build();
        superCertId = Peer.CertId.newBuilder().setCreditCode(super_credit).setCertName("super_admin").build();
    }

    @Test
    @DisplayName("部署RegulateTPL合约")
    @Order(1)
    void deployTest() throws IOException, InterruptedException {
        String contractStr = FileUtils.readFileToString(new File("jks/test/tpl/RegulateTPL.scala"), StandardCharsets.UTF_8);
        Peer.ChaincodeDeploy chaincodeDeploy = Peer.ChaincodeDeploy.newBuilder()
            .setCType(Peer.ChaincodeDeploy.CodeType.CODE_SCALA)
            .setCodePackage(contractStr)
            .build();
        Peer.Transaction transaction = superCreator.createDeployTran(superCertId, regulateTPL, chaincodeDeploy, 0, "");
        JSONObject result = tranPostClient.postSignedTran(transaction);
        Assertions.assertTrue((null == result.get("err")) || "存在重复的合约Id".equals(result.getJSONObject("err").get("msg")));
        Thread.sleep(5000);
    }

    @Test
    @DisplayName("注册RegulateTPL合约操作")
    @Order(2)
    void signOperateTest() throws InvalidProtocolBufferException, InterruptedException {
        long millis = System.currentTimeMillis();
        Peer.Operate operate = Peer.Operate.newBuilder()
            .setOpId(DigestUtils.sha256Hex(network + "RegulateTPL.regBlocks"))
            .setDescription("注册RegulateTPL.regBlocks方法")
            .setRegister(super_credit)
            .setIsPublish(false)
            .setOperateType(Peer.Operate.OperateType.OPERATE_CONTRACT)
            .setAuthFullName(network + "RegulateTPL.regBlocks")
            .setCreateTime(Timestamp.newBuilder().setSeconds(millis / 1000).setNanos((int) ((millis % 1000) * 1000000)).build())
            .setOpValid(true)
            .setVersion("1.0")
            .build();
        Peer.Transaction transaction = superCreator.createInvokeTran(UUID.randomUUID().toString(), superCertId, didChaincodeId,
            "signUpOperate", JsonFormat.printer().print(operate), 0, "");
        JSONObject result = tranPostClient.postSignedTran(transaction);
        Assertions.assertTrue((null == result.get("err")) || "operate已存在".equals(result.getJSONObject("err").getJSONObject("msg").get("reason")));
    }

    @Test
    @DisplayName("调用putProof方法上链一条违规交易")
    @Order(3)
    void invokeIllegalTest() throws InterruptedException {
        Map<String, String> map = new HashMap<>();
        map.put(UUID.randomUUID().toString(), "假设这是一条违规内容，违反法律法规或公序良俗！");
        Peer.Transaction transaction = superCreator.createInvokeTran(UUID.randomUUID().toString(), superCertId, contractAssetsTPL,
            "putProof", JSON.toJSONString(map), 0, "");
        JSONObject result = tranPostClient.postSignedTran(transaction);
        logger.info("{}", result);
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.get("err"));
        Thread.sleep(5000);

        ChainInfoClient.TranInfoAndHeight infoAndHeight = infoClient.getTranInfoAndHeightByTranId(transaction.getId());
        Peer.Block block = infoClient.getBlockByHeight(infoAndHeight.getHeight());
        illegal_tran_id = transaction.getId();
        illegal_block_height = infoAndHeight.getHeight();
        illegal_block_hash = block.getHeader().getHashPresent().toStringUtf8();
    }

    @Test
    @DisplayName("调用regBlocks对违规内容监管")
    @Order(4)
    void regBlocksTest() throws InterruptedException {
        logger.info("illegal_tran_id:{}", illegal_tran_id);
        logger.info("illegal_block_height:{}", illegal_block_height);
        logger.info("illegal_block_hash:{}", illegal_block_hash);
        List<Conclusion> conclusions = new ArrayList<>();
        Map<String, String> txids = new HashMap<>();
        txids.put(illegal_tran_id, "内容违规");
        Conclusion conclusion = new Conclusion();
        conclusion.setHeight(illegal_block_height);
        conclusion.setBlockHash(illegal_block_hash);
        conclusion.setIllegalTrans(txids);
        conclusions.add(conclusion);
        logger.info("conclusions:{}", conclusions);
        List<Conclusion> conclusionList = new ArrayList<>();
        conclusionList.add(conclusion);
        Peer.Transaction transaction = superCreator.createInvokeTran(UUID.randomUUID().toString(), superCertId, regulateTPL,
            "regBlocks", JSON.toJSONString(conclusionList), 0, "");
        JSONObject result = tranPostClient.postSignedTran(transaction);
        Assertions.assertNotNull(result);
        Assertions.assertNull(result.get("err"));
        Thread.sleep(2000);
    }

    @Test
    @DisplayName("测试违规交易被监管后内容是否被隐藏")
    @Order(5)
    void getIllegalData() {
        // 根据交易ID查询交易内容
        Peer.Transaction tran = infoClient.getTranByTranId(illegal_tran_id);
        logger.info("\n{}", tran.getIpt());
        Assertions.assertEquals(tran.getId(), tran.getIpt().getArgs(0));
        Assertions.assertEquals(2, tran.getIpt().getArgsCount());

        // 根据交易ID查询交易内容及所在高度
        Peer.Transaction tranInfo = infoClient.getTranInfoAndHeightByTranId(illegal_tran_id).getTranInfo();
        Assertions.assertEquals(tranInfo.getId(), tranInfo.getIpt().getArgs(0));
        Assertions.assertEquals(2, tranInfo.getIpt().getArgsCount());

        // 根据区块高度查询区块内容
        Peer.Block block = infoClient.getBlockByHeight(illegal_block_height);
        block.getTransactionsList().forEach(t -> {
            if (illegal_tran_id.equals(t.getId())) {
                logger.info("\n{}", t.getIpt());
                Assertions.assertEquals(t.getId(), t.getIpt().getArgs(0));
                Assertions.assertEquals(2, t.getIpt().getArgsCount());
            }
        });

        // 根据区块hash查询区块内容
        Peer.Block block1 = infoClient.getBlockByBlockHash(block.getHeader().getHashPresent());
        block1.getTransactionsList().forEach(t -> {
            if (illegal_tran_id.equals(t.getId())) {
                logger.info("\n{}", t.getIpt());
                Assertions.assertEquals(t.getId(), t.getIpt().getArgs(0));
                Assertions.assertEquals(2, t.getIpt().getArgsCount());
            }
        });
    }
}
