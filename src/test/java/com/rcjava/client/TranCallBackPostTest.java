package com.rcjava.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rcjava.client.callback.TranCallBack;
import com.rcjava.model.Transfer;
import com.rcjava.protos.Peer.CertId;
import com.rcjava.protos.Peer.ChaincodeId;
import com.rcjava.protos.Peer.Transaction;
import com.rcjava.tran.TranCreator;
import com.rcjava.util.CertUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author zyf
 */
public class TranCallBackPostTest implements TranCallBack {

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
    void testPostTranByStream() throws InterruptedException {

        String tranId = UUID.randomUUID().toString().replace("-", "");

        List params = new ArrayList<String>();
        params.add(JSON.toJSONString(transfer));
        Transaction tran = tranCreator.createInvokeTran(tranId, certId, contractAssetsId, "transfer", params);
        // 超时时间为1分钟
        tranPostClient.postSignedTran(tran, this, 1);
//        assertThat(res).containsKey("txid");
        Thread.sleep(100000);
    }

    @Override
    public void preTransactionResult(String txid, boolean flag, JSONObject result) {
        logger.info("交易 {} 预执行的结果：{}", txid, result);
    }

    @Override
    public void isOnChain(String txid, boolean result) {
        logger.info("交易 {} 上链的结果：{}", txid, result);
    }
}
