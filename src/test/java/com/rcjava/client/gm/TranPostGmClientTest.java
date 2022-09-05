package com.rcjava.client.gm;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.rcjava.client.TranPostClient;
import com.rcjava.gm.GMProvider;
import com.rcjava.model.Transfer;
import com.rcjava.protos.Peer.CertId;
import com.rcjava.protos.Peer.ChaincodeId;
import com.rcjava.protos.Peer.Transaction;
import com.rcjava.tran.TranCreator;
import com.rcjava.util.CertUtil;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContextBuilder;
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

import static com.google.common.truth.Truth.assertThat;

/**
 * @author zyf
 */
public class TranPostGmClientTest extends GMProvider {

    private Logger logger = LoggerFactory.getLogger(getClass());

    SSLContext sslContext = SSLContextBuilder.create()
            .setProtocol("GMSSLv1.1").setProvider("BCJSSE")
            .setKeyStoreType("PKCS12")
            .setKeyManagerFactoryAlgorithm("PKIX")
            .loadTrustMaterial(new File("pfx/mytruststore.pfx"), "changeme".toCharArray(), new TrustSelfSignedStrategy())
            .loadKeyMaterial(new File("pfx/215159697776981712.node1.pfx"), "123".toCharArray(), "123".toCharArray())
            .build();

    private TranPostClient tranPostClient = new TranPostClient("192.168.2.69:9081", sslContext);

    private Transfer transfer = new Transfer("identity-net:215159697776981712", "identity-net:904703631549900672", 5);

    private CertId certId = CertId.newBuilder().setCreditCode("identity-net:215159697776981712").setCertName("node1").build(); // 签名ID
    //这个是给转账交易示范用的，此ID需要与repchain合约部署的一致
    private ChaincodeId contractAssetsId = ChaincodeId.newBuilder().setChaincodeName("ContractAssetsTPL").setVersion(1).build();

    private PrivateKey privateKey = CertUtil.genGmX509CertPrivateKey(
            new File("pfx/215159697776981712.node1.pfx"), "123", "Sig"
    ).getPrivateKey();

    // 交易的签名算法根据对应RepChain版本进行设置
    private TranCreator tranCreator = TranCreator.newBuilder().setPrivateKey(privateKey).setSignAlgorithm("SM3WITHSM2").build();

    public TranPostGmClientTest() throws Exception {
    }

    @Test
    @DisplayName("测试提交交易-流式")
    void testPostTranByStream() {

        String tranId = UUID.randomUUID().toString().replace("-", "");

        List params = new ArrayList<String>();
        params.add(JSON.toJSONString(transfer));
        Transaction tran = tranCreator.createInvokeTran(tranId, certId, contractAssetsId, "transfer", params, 0, "");
        tranPostClient.setUseJavaImpl(true);
        JSONObject res = tranPostClient.postSignedTran(tran);

        assertThat(res).containsKey("txid");
        assertThat(res.getString("txid")).isEqualTo(tran.getId());

        logger.info("txid: {}", tran.getId());
    }

    @Test
    @DisplayName("测试提交交易-字符串")
    void testPostTranByString() {

        String tranId = UUID.randomUUID().toString().replace("-", "");

        Transaction tran = tranCreator.createInvokeTran(tranId, certId, contractAssetsId, "transfer", JSON.toJSONString(transfer), 0, "");
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

}
