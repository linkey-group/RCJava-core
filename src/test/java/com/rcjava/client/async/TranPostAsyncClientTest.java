package com.rcjava.client.async;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.rcjava.model.Transfer;
import com.rcjava.protos.Peer.CertId;
import com.rcjava.protos.Peer.ChaincodeId;
import com.rcjava.protos.Peer.Transaction;
import com.rcjava.tran.TranCreator;
import com.rcjava.util.CertUtil;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpResponse;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContexts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;

import static com.google.common.truth.Truth.assertThat;
import static java.lang.Thread.sleep;

/**
 * @author zyf
 */
public class TranPostAsyncClientTest {

    private Logger logger = LoggerFactory.getLogger(getClass());

    SSLContext sslContext = SSLContexts.custom()
            .loadTrustMaterial(new File("jks/jdk13/121000005l35120456.node1.jks"), "123".toCharArray(), new TrustSelfSignedStrategy())
            .loadKeyMaterial(new File("jks/jdk13/121000005l35120456.node1.jks"), "123".toCharArray(), "123".toCharArray())
            .build();

//    private TranPostAsyncClient tranPostClient = new TranPostAsyncClient("localhost:9081", sslContext);
    private TranPostAsyncClient tranPostClient = new TranPostAsyncClient("localhost:9081");

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

    public TranPostAsyncClientTest() throws Exception {
    }

    @Test
    @DisplayName("测试提交交易-流式")
    void testPostTranByStream() throws InterruptedException {

        String tranId = UUID.randomUUID().toString().replace("-", "");

        List<String> params = new ArrayList<>();
        params.add(JSON.toJSONString(transfer));
        Transaction tran = tranCreator.createInvokeTran(tranId, certId, contractAssetsId, "transfer", params, 0, "");
        Future<HttpResponse> responseFuture = tranPostClient.postSignedTran(tran);

        sleep(5000);

        JSONObject result = TranPostAsyncClient.resolveHttpResponseFuture(responseFuture);

        System.out.println(result);

        assertThat(result).containsKey("txid");

    }

    @Test
    @DisplayName("测试提交交易-字符串")
    void testPostTranByString() throws InterruptedException {

        String tranId = UUID.randomUUID().toString().replace("-", "");

        Transaction tran = tranCreator.createInvokeTran(tranId, certId, contractAssetsId, "transfer", JSON.toJSONString(transfer), 0, "");
        String tranHex = Hex.encodeHexString(tran.toByteArray());

        Future<HttpResponse> responseFuture = tranPostClient.postSignedTran(tranHex);

        sleep(5000);

        JSONObject result = TranPostAsyncClient.resolveHttpResponseFuture(responseFuture);

        System.out.println(result);

        assertThat(result.getString("txid")).isEqualTo(tran.getId());

    }

}
