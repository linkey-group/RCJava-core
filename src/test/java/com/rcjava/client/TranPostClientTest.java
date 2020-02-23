package com.rcjava.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rcjava.model.Transfer;
import com.rcjava.protos.Peer.CertId;
import com.rcjava.protos.Peer.ChaincodeId;
import com.rcjava.protos.Peer.Transaction;
import com.rcjava.tran.TranCreator;
import com.rcjava.util.CertUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author zyf
 */
public class TranPostClientTest {

    private TranPostClient tranPostClient = new TranPostClient("localhost:8081");

    private Transfer transfer = new Transfer("121000005l35120456", "12110107bi45jh675g", 5);

    @Test
    @DisplayName("测试提交交易-流式")
    void testPostTranByStream() {

        CertId certId = CertId.newBuilder().setCreditCode("121000005l35120456").setCertName("node1").build(); // 签名ID
        //这个是给转账交易示范用的，此ID需要与repchain合约部署的一致
        ChaincodeId contractAssetsId = ChaincodeId.newBuilder().setChaincodeName("ContractAssetsTPL").setVersion(1).build();

        PrivateKey privateKey = CertUtil.genX509CertPrivateKey(
                new File("jks/121000005l35120456.node1.jks"),
                "123",
                "121000005l35120456.node1").getPrivateKey();
        TranCreator tranCreator = TranCreator.newBuilder()
                .setPrivateKey(privateKey)
                .setSignAlgorithm("sha1withecdsa")
                .build();

        String tranId = UUID.randomUUID().toString().replace("-", "");

        List params = new ArrayList<String>();
        params.add(JSON.toJSONString(transfer));
        Transaction tran = tranCreator.createInvokeTran(tranId, certId, contractAssetsId, "transfer", params);
        JSONObject res = tranPostClient.postTranByStream(tran);

        assertThat(res).containsKey("txid");
    }

    // TODO 测试字符串提交交易的方法
    @Test
    @DisplayName("测试提交交易-字符串")
    void testPostTranByString() {

    }
}
