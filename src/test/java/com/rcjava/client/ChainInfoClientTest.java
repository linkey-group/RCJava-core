package com.rcjava.client;

import com.rcjava.protos.Peer;
import com.rcjava.protos.Peer.Block;
import com.rcjava.protos.Peer.BlockchainInfo;
import com.rcjava.sign.impl.ECDSASign;
import com.rcjava.util.CertUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.security.PublicKey;

import static com.google.common.truth.Truth.assertThat;


/**
 * @author zyf
 */
public class ChainInfoClientTest {

    private ChainInfoClient chainInfoClient = new ChainInfoClient("localhost:9081");

    @Test
    @DisplayName("测试获取链相关信息")
    void testGetChainInfo() {
        BlockchainInfo blockchainInfo = chainInfoClient.getChainInfo();
        System.out.println(blockchainInfo.getHeight());
    }

    @Test
    @DisplayName("测试根据高度获取块")
    void testGetBlockByHeight() {
        Block block = chainInfoClient.getBlockByHeight(5);
        Peer.BlockHeader header = block.getHeader();
        assertThat(header.getHeight()).isEqualTo(5);
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
        Object res = chainInfoClient.queryDB("net1", "ContractAssetsTPL", "", "121000005l35120456");
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
        Block block = chainInfoClient.getBlockByHeight(5);
        Peer.BlockHeader header = block.getHeader();
        assertThat(header.getHeight()).isEqualTo(5);
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
        Peer.Transaction tran = chainInfoClient.getTranByTranId("d8ed4810-615f-4f8f-bf94-21b5a98ebf97");
        byte[] sigData = tran.getSignature().getSignature().toByteArray();
        Peer.Transaction tranWithOutSig = tran.toBuilder().clearSignature().build();
        PublicKey publicKey = CertUtil.genX509CertPrivateKey(
                new File("jks/jdk13/121000005l35120456.node1.jks"),
                "123",
                "121000005l35120456.node1").getCertificate().getPublicKey();
        System.out.println(new ECDSASign("sha256withecdsa").verify(sigData, tranWithOutSig.toByteArray(), publicKey));
    }
}
