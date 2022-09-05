package com.rcjava.client.gm;

import com.rcjava.client.ChainInfoClient;
import com.rcjava.gm.GMProvider;
import com.rcjava.protos.Peer;
import com.rcjava.protos.Peer.Block;
import com.rcjava.protos.Peer.BlockchainInfo;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import java.io.File;

import static com.google.common.truth.Truth.assertThat;


/**
 * @author zyf
 */
public class ChainInfoGmClientTest extends GMProvider {

    SSLContext sslContext = SSLContextBuilder.create()
            .setProtocol("GMSSLv1.1").setProvider("BCJSSE")
            .setKeyStoreType("PKCS12")
            .setKeyManagerFactoryAlgorithm("PKIX")
            .loadTrustMaterial(new File("pfx/mytruststore.pfx"), "changeme".toCharArray(), new TrustSelfSignedStrategy())
            .loadKeyMaterial(new File("pfx/215159697776981712.node1.pfx"), "123".toCharArray(), "123".toCharArray())
            .build();

    private ChainInfoClient chainInfoClient = new ChainInfoClient("192.168.2.69:9081", sslContext);

    public ChainInfoGmClientTest() throws Exception {
    }

    @Test
    @DisplayName("测试获取链相关信息")
    void testGetChainInfo() {
        BlockchainInfo blockchainInfo = chainInfoClient.getChainInfo();
        System.out.println(blockchainInfo.getHeight());
    }

    @Test
    @DisplayName("测试根据高度获取块")
    void testGetBlockByHeight() {
        Block block = chainInfoClient.getBlockByHeight(1);
        Peer.BlockHeader header = block.getHeader();
        assertThat(header.getHeight()).isEqualTo(1);
    }

    @Test
    @DisplayName("测试根据交易ID获取入块时间")
    void testGetBlockTimeByTranId() {
        ChainInfoClient.CreateTime createTime = chainInfoClient.getBlockTimeByTranId("b4886521-20bc-424e-b58f-609f4da12ad2");
        if (null != createTime) {
            System.out.println(createTime.getCreateTime());
            System.out.println(createTime.getCreateTimeUtc());
        }
    }

    @Test
    @DisplayName("查询RepChain的leveldb/rocksdb中的数据")
    void testQueryDB() {
        Object res = chainInfoClient.queryDB("identity-net", "ContractAssetsTPL", "", "215159697776981712");
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
        ChainInfoClient.TranInfoAndHeight tranInfoAndHeight = chainInfoClient.getTranInfoAndHeightByTranId("1234567890");
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
        System.out.println(block);
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


}
