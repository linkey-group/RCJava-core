package com.rcjava.client;

import com.rcjava.protos.Peer.Block;
import com.rcjava.protos.Peer.BlockchainInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;


/**
 * @author zyf
 */
public class ChainInfoClientTest {

    private ChainInfoClient chainInfoClient = new ChainInfoClient("localhost:8081");

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
        assertThat(block.getHeight()).isEqualTo(5);
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
}
