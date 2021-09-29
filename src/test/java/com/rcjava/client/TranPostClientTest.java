package com.rcjava.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rcjava.model.Transfer;
import com.rcjava.protos.Peer;
import com.rcjava.protos.Peer.CertId;
import com.rcjava.protos.Peer.ChaincodeId;
import com.rcjava.protos.Peer.Transaction;
import com.rcjava.tran.TranCreator;
import com.rcjava.tran.impl.DeployTran;
import com.rcjava.util.CertUtil;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author zyf
 */
public class TranPostClientTest {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private TranPostClient tranPostClient = new TranPostClient("localhost:8081");

    private Transfer transfer = new Transfer("121000005l35120456", "12110107bi45jh675g", 5);

    private CertId certId = CertId.newBuilder().setCreditCode("121000005l35120456").setCertName("node1").build(); // 签名ID
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

    @Test
    @DisplayName("测试提交交易-流式")
    void testPostTranByStream() {

        String tranId = UUID.randomUUID().toString().replace("-", "");

        List params = new ArrayList<String>();
        params.add(JSON.toJSONString(transfer));
        Transaction tran = tranCreator.createInvokeTran(tranId, certId, contractAssetsId, "transfer", params);
        JSONObject res = tranPostClient.postSignedTran(tran);

        assertThat(res).containsKey("txid");

        logger.info("测试日志文件");
    }

    @Test
    @DisplayName("测试提交交易-字符串")
    void testPostTranByString() {

        String tranId = UUID.randomUUID().toString().replace("-", "");

        Transaction tran = tranCreator.createInvokeTran(tranId, certId, contractAssetsId, "transfer", JSON.toJSONString(transfer));
        String tranHex = Hex.encodeHexString(tran.toByteArray());

        JSONObject res = tranPostClient.postSignedTran(tranHex);

        assertThat(res.getString("txid")).isEqualTo(tran.getId());
    }

    @Test
    @DisplayName("测试提交交易-流式，使用Java实现")
    void testPostTranByStreamUseJavaImpl() {

        String tranId = UUID.randomUUID().toString().replace("-", "");

        List params = new ArrayList<String>();
        params.add(JSON.toJSONString(transfer));
        Transaction tran = tranCreator.createInvokeTran(tranId, certId, contractAssetsId, "transfer", params);
        tranPostClient.setUseJavaImpl(true);
        JSONObject res = tranPostClient.postSignedTran(tran);
        tranPostClient.setUseJavaImpl(false);

        assertThat(res).containsKey("txid");

        logger.info("测试日志文件");
    }

    @Test
    @DisplayName("测试提交交易-字符串，使用Java实现")
    void testPostTranByStringUseJavaImpl() {

        String tranId = UUID.randomUUID().toString().replace("-", "");

        Transaction tran = tranCreator.createInvokeTran(tranId, certId, contractAssetsId, "transfer", JSON.toJSONString(transfer));
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
        DeployTran deployTran = DeployTran.newBuilder()
                .setTxid(DigestUtils.sha256Hex(tplString))
                .setCertId(certId)
                .setChaincodeId(customTplId)
                .setSpcPackage(tplString)
                .setLegal_prose("")
                .setTimeout(5000)
                .setCodeType(Peer.ChaincodeDeploy.CodeType.CODE_SCALA)
                .build();
        Peer.Transaction signedDeployTran = tranCreator.createDeployTran(deployTran);
        JSONObject deployRes = tranPostClient.postSignedTran(signedDeployTran);
        System.out.println(deployRes);
    }
}
