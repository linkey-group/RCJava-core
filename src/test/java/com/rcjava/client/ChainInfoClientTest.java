package com.rcjava.client;

import com.alibaba.fastjson2.JSONObject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.rcjava.protos.Peer;
import com.rcjava.protos.Peer.Block;
import com.rcjava.protos.Peer.BlockchainInfo;
import com.rcjava.sign.impl.ECDSASign;
import com.rcjava.util.CertUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContexts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.security.*;

import static com.google.common.truth.Truth.assertThat;
import static com.rcjava.util.StateUtil.toInstance;
import static com.rcjava.util.StateUtil.toJsonString;


/**
 * @author zyf
 */
public class ChainInfoClientTest {

    SSLContext sslContext = SSLContexts.custom()
            .loadTrustMaterial(new File("jks/jdk13/121000005l35120456.node1.jks"), "123".toCharArray(), new TrustSelfSignedStrategy())
            .loadKeyMaterial(new File("jks/jdk13/121000005l35120456.node1.jks"), "123".toCharArray(), "123".toCharArray())
            .build();

//    private ChainInfoClient chainInfoClient = new ChainInfoClient("localhost:9081", sslContext);
    private ChainInfoClient chainInfoClient = new ChainInfoClient("localhost:9081");

    public ChainInfoClientTest() throws Exception {
    }

    @Test
    @DisplayName("测试获取链相关信息")
    void testGetChainInfo() {
        BlockchainInfo blockchainInfo = chainInfoClient.getChainInfo();
        System.out.println(blockchainInfo.getHeight());
    }

    @Test
    @DisplayName("测试根据高度获取块")
    void testGetBlockByHeight() throws Exception{
        Block block = chainInfoClient.getBlockByHeight(1);
        Peer.BlockHeader header = block.getHeader();
        System.out.println(Base64.encodeBase64String(block.getHeader().getHashPresent().toByteArray()));
        System.out.println(JsonFormat.printer().print(block));
        assertThat(header.getHeight()).isEqualTo(1);
    }

    @Test
    @DisplayName("测试根据高度获取区块头")
    void testGetBlockHeaderByHeight() throws Exception{
        Peer.BlockHeader blockHeader = chainInfoClient.getBlockHeaderByHeight(1);
        System.out.println(Base64.encodeBase64String(blockHeader.getHashPresent().toByteArray()));
        System.out.println(JsonFormat.printer().print(blockHeader));
        assertThat(blockHeader.getHeight()).isEqualTo(1);
    }

    @Test
    @DisplayName("测试根据交易ID获取入块时间")
    void testGetBlockTimeByTranId() {
        ChainInfoClient.CreateTime createTime = chainInfoClient.getBlockTimeByTranId("d8ed4810-615f-4f8f-bf94-21b5a98ebf97");
        if (null != createTime) {
            System.out.println(createTime.getCreateTime());
            System.out.println(createTime.getCreateTimeUtc());
        }
    }

    @Test
    @DisplayName("查询RepChain的leveldb/rocksdb中的数据")
    void testQueryDB() {
        Object res = chainInfoClient.queryDB("identity-net", "ContractAssetsTPL", "", "121000005l35120456");
        System.out.println(res);
    }

    @Test
    @DisplayName("测试获取nodes数量")
    void testGetNodesNum() {
        assertThat(chainInfoClient.getChainInfoNode().getNodes()).isEqualTo(5);
    }

    @Test
    @DisplayName("根据交易id获取交易及交易所在区块高度")
    void testGetTranInfoAndHeight() {
        ChainInfoClient.TranInfoAndHeight tranInfoAndHeight = chainInfoClient.getTranInfoAndHeightByTranId("123");
        assertThat(tranInfoAndHeight).isNull();
    }

    @Test
    @DisplayName("测试根据高度获取块，使用Java实现")
    void testGetBlockByHeightUseJavaImpl() {
        chainInfoClient.setUseJavaImpl(true);
        Block block = chainInfoClient.getBlockByHeight(1);
        Peer.BlockHeader header = block.getHeader();
        assertThat(header.getHeight()).isEqualTo(1);
        chainInfoClient.setUseJavaImpl(false);
    }

    @Test
    @DisplayName("测试根据高度获取块，使用Java实现")
    void testGetBlockStreamByHeightUseJavaImpl() {
        chainInfoClient.setUseJavaImpl(true);
        Block block = chainInfoClient.getBlockStreamByHeight(1);
        Peer.BlockHeader header = block.getHeader();
        assertThat(header.getHeight()).isEqualTo(1);
        chainInfoClient.setUseJavaImpl(false);
    }

    @Test
    @DisplayName("测试验签，使用与签名交易对应的证书或公钥来验签即可")
    void testVerify() {
        Peer.Transaction tran = chainInfoClient.getTranByTranId("5c1bcd72-2b2e-48d3-a8c5-28aa5cf36821");
        byte[] sigData = tran.getSignature().getSignature().toByteArray();
        Peer.Transaction tranWithOutSig = tran.toBuilder().clearSignature().build();
        PublicKey publicKey = CertUtil.genX509CertPrivateKey(
                new File("jks/jdk13/121000005l35120456.node1.jks"),
                "123",
                "121000005l35120456.node1").getCertificate().getPublicKey();
        System.out.println(new ECDSASign("sha256withecdsa").verify(sigData, tranWithOutSig.toByteArray(), publicKey));
    }

    @Test
    void testGetBlock () throws InvalidProtocolBufferException {
        String blockUrl = "http://127.0.0.1:8081/block/1";
        BaseClient client = new RCJavaClient();
        JSONObject jsonObject = client.getJObject(blockUrl);
        Block.Builder builder = Block.newBuilder();
        JsonFormat.parser().merge(jsonObject.toJSONString(), builder);
        Block block = builder.build();
        assertThat(block.getHeader().getHeight()).isEqualTo(1);
    }

    @DisplayName("参考README，如果要解析RepChain中定义的protobuf结构，需要安装rc-proto.jar")
    @Test
    void testTransactionResult() {
        Peer.TransactionResult result = chainInfoClient.getTranResultByTranId("RdidOperateAuthorizeTPL___signer-identity-net");
        ChainInfoClient.TranAndTranResult tranAndTranResult = chainInfoClient.getTranAndResultByTranId("1376cbbf-edc1-463b-82af-e643d2257159");
        byte[] bytes = result.getStatesSetMap().get("identity-net_RdidOperateAuthorizeTPL___signer-identity-net:951002007l78123233").toByteArray();
        String json = toJsonString(bytes);
        Object object = toInstance(bytes);
        System.out.println(json);
        System.out.println(object);
    }
}
