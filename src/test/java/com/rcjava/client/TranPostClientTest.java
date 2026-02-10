package com.rcjava.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.rcjava.model.Transfer;
import com.rcjava.protos.Peer;
import com.rcjava.protos.Peer.CertId;
import com.rcjava.protos.Peer.ChaincodeId;
import com.rcjava.protos.Peer.Transaction;
import com.rcjava.tran.TranCreator;
import com.rcjava.tran.impl.DeployTran;
import com.rcjava.util.CertUtil;
import com.rcjava.util.StateUtil;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContexts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author zyf
 */
public class TranPostClientTest {

    private Logger logger = LoggerFactory.getLogger(getClass());

    SSLContext sslContext = SSLContexts.custom()
            .loadTrustMaterial(new File("jks/jdk13/121000005l35120456.node1.jks"), "123".toCharArray(), new TrustSelfSignedStrategy())
            .loadKeyMaterial(new File("jks/jdk13/121000005l35120456.node1.jks"), "123".toCharArray(), "123".toCharArray())
            .build();

    private TranPostClient tranPostClient = new TranPostClient("localhost:9081");
//    private TranPostClient tranPostClient = new TranPostClient("localhost:9081", sslContext);

    private Transfer transfer = new Transfer("identity-net:121000005l35120456", "identity-net:12110107bi45jh675g", 5);

    private CertId certId = CertId.newBuilder().setCreditCode("identity-net:121000005l35120456").setCertName("node1").build(); // 签名ID
    //这个是给转账交易示范用的，此ID需要与repchain合约部署的一致
    private ChaincodeId contractAssetsId = ChaincodeId.newBuilder().setChaincodeName("ContractAssetsTPL").setVersion(1).build();

    private PrivateKey privateKey = CertUtil.genX509CertPrivateKey(
            new File("jks/jdk13/121000005l35120456.node1.jks"),
            "123",
            "121000005l35120456.node1").getPrivateKey();

    // 交易的签名算法根据对应RepChain版本进行设置
    private TranCreator tranCreator = TranCreator.newBuilder()
            .setPrivateKey(privateKey)
            .setSignAlgorithm("sha256withecdsa")
            .build();

    public TranPostClientTest() throws Exception {
    }

    @Test
    @DisplayName("测试提交交易-流式")
    void testPostTranByStream() {

        String tranId = UUID.randomUUID().toString().replace("-", "");

        List params = new ArrayList<String>();
        params.add(JSON.toJSONString(transfer));
        Transaction tran = tranCreator.createInvokeTran(tranId, certId, contractAssetsId, "transfer", params, 0, "");
        JSONObject res = tranPostClient.postSignedTran(tran);

        assertThat(res).containsKey("txid");
        assertThat(res.getString("txid")).isEqualTo(tran.getId());

        logger.info("txid: {}", tran.getId());
    }

    @Test
    @DisplayName("测试提交交易-字符串，写操作")
    void testPostTranByString() {

        String tranId = UUID.randomUUID().toString().replace("-", "");

        Transaction tran = tranCreator.createInvokeTran(tranId, certId, contractAssetsId, "transfer", JSON.toJSONString(transfer), 0, "");
        String tranHex = Hex.encodeHexString(tran.toByteArray());

        JSONObject res = tranPostClient.postSignedTran(tranHex);

        assertThat(res.getString("txid")).isEqualTo(tran.getId());
    }    
    
    @Test
    @DisplayName("测试提交交易-字符串，只读操作")
    void testQueryTranByString() {

        String tranId = UUID.randomUUID().toString().replace("-", "");

        Transaction tran = tranCreator.createInvokeTran(tranId, certId, contractAssetsId, "transfer", JSON.toJSONString(transfer), 0, "");
        String tranHex = Hex.encodeHexString(tran.toByteArray());

        JSONObject res = tranPostClient.querySignedTran(tranHex);

        System.out.println(res);
        System.out.println(res.getJSONObject("result"));

        JSONObject postResult = res.getJSONObject("result");
        Peer.TransactionResult.Builder transactionResultBuilder = Peer.TransactionResult.newBuilder();
        String tranResultJson = postResult.getJSONObject("data").toJSONString();
        try {
            JsonFormat.parser().merge(tranResultJson, transactionResultBuilder);
        } catch (InvalidProtocolBufferException e) {
            logger.error("construct TransactionResult occurs error, errorMsg is {}", e.getMessage(), e);
        }
        Peer.TransactionResult transactionResult = transactionResultBuilder.build();

        transactionResult.getStatesGetMap().forEach((key, value) -> {
            if (!key.contains("signer")) {
                Integer valueStr = StateUtil.toInstance(value.toByteArray(), Integer.class);
                System.out.println(key + ": " + valueStr);
            }
        });
    }

    @Test
    @DisplayName("测试提交交易-流式，使用Java实现")
    void testPostTranByStreamUseJavaImpl() {

        String tranId = UUID.randomUUID().toString().replace("-", "");

        List params = new ArrayList<String>();
        params.add(JSON.toJSONString(transfer));
        Transaction tran = tranCreator.createInvokeTran(tranId, certId, contractAssetsId, "transfer", params, 0, "");
        tranPostClient.setUseJavaImpl(true);
        JSONObject res = tranPostClient.postSignedTran(tran);
        tranPostClient.setUseJavaImpl(false);

        assertThat(res).containsKey("txid");

        logger.info("txid: {}", tran.getId());
    }

    @Test
    @DisplayName("测试提交交易-字符串，使用Java实现")
    void testPostTranByStringUseJavaImpl() {

        String tranId = UUID.randomUUID().toString().replace("-", "");

        Transaction tran = tranCreator.createInvokeTran(tranId, certId, contractAssetsId, "transfer", JSON.toJSONString(transfer), 0, "");
        String tranHex = Hex.encodeHexString(tran.toByteArray());
        tranPostClient.setUseJavaImpl(true);
        JSONObject res = tranPostClient.postSignedTran(tranHex);
        tranPostClient.setUseJavaImpl(false);

        assertThat(res.getString("txid")).isEqualTo(tran.getId());
    }

    @Test
    @DisplayName("测试部署合约")
    void testDeployContractTplT() throws IOException {
        // CustomTPL.scala 是事先编写好的合约文件
        Peer.ChaincodeId customTplId = Peer.ChaincodeId.newBuilder().setChaincodeName("CustomProofTPL").setVersion(1).build();
        String tplString = FileUtils.readFileToString(new File("tpl/CustomProofTPL.scala"), StandardCharsets.UTF_8);
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
                .setTxid(DigestUtils.sha256Hex(tplString))
                .setCertId(certId)
                .setChaincodeId(customTplId)
                .setChaincodeDeploy(chaincodeDeploy)
                .build();
        Peer.Transaction signedDeployTran = tranCreator.createDeployTran(deployTran);
        JSONObject deployRes = tranPostClient.postSignedTran(signedDeployTran);
        System.out.println(deployRes);
    }

    @Test
    @DisplayName("测试提交交易-字符串，写操作")
    void testPostTranByString_1() {

        String tranId = UUID.randomUUID().toString().replace("-", "");
        HashMap<String, String> proof = new HashMap<>();
//        proof.put(UUID.randomUUID().toString(), "123-TEST");
        proof.put("511876b4-9567-44a7-8fab-8e1cf6ce260fk", "1234-TEST");

        Transaction tran = tranCreator.createInvokeTran(tranId, certId, contractAssetsId, "putProof", JSON.toJSONString(proof), 0, "");
        String tranHex = Hex.encodeHexString(tran.toByteArray());

        JSONObject res = tranPostClient.postSignedTran(tranHex);

        assertThat(res.getString("txid")).isEqualTo(tran.getId());
    }

    @Test
    @DisplayName("测试提交交易-字符串，只读操作")
    void testQueryTranByString_2() {

        String tranId = UUID.randomUUID().toString().replace("-", "");
        HashMap<String, String> proof = new HashMap<>();
        proof.put("511876b4-9567-44a7-8fab-8e1cf6ce260fk", "123-TEST");

        Transaction tran = tranCreator.createInvokeTran(tranId, certId, contractAssetsId, "putProof", JSON.toJSONString(proof), 0, "");
        String tranHex = Hex.encodeHexString(tran.toByteArray());

        JSONObject res = tranPostClient.querySignedTran(tranHex);

        System.out.println(res);
        System.out.println(res.getJSONObject("result"));

        Peer.TransactionResult.Builder transactionResultBuilder = Peer.TransactionResult.newBuilder();
        String tranResultJson = res.getJSONObject("result").getJSONObject("data").toJSONString();
        try {
            JsonFormat.parser().merge(tranResultJson, transactionResultBuilder);
        } catch (InvalidProtocolBufferException e) {
            logger.error("construct TransactionResult occurs error, errorMsg is {}", e.getMessage(), e);
        }
        Peer.TransactionResult transactionResult = transactionResultBuilder.build();

        transactionResult.getStatesGetMap().forEach((key, value) -> {
            String valueStr = StateUtil.toInstance(value.toByteArray(), String.class);
            System.out.println(key + ": " + valueStr);
        });
    }
}
