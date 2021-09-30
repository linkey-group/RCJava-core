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

    @Test
    @DisplayName("测试根据高度获取块，使用Java实现")
    void testGetBlockByHeightUseJavaImpl() {
        chainInfoClient.setUseJavaImpl(true);
        Block block = chainInfoClient.getBlockByHeight(5);
        assertThat(block.getHeight()).isEqualTo(5);
        chainInfoClient.setUseJavaImpl(false);
    }

    @Test
    @DisplayName("测试根据高度获取块，使用Java实现")
    void testGetBlockStreamByHeightUseJavaImpl() {
        chainInfoClient.setUseJavaImpl(true);
        Block block = chainInfoClient.getBlockStreamByHeight(0);
        assertThat(block.getHeight()).isEqualTo(0);
        chainInfoClient.setUseJavaImpl(false);
    }

    @Test
    @DisplayName("测试验签，使用与签名交易对应的证书或公钥来验签即可")
    void testVerify() {
        Peer.Transaction tran = chainInfoClient.getTranByTranId("542a1649-d74c-4696-8aa8-f37050b05b69");
        byte[] sigData = tran.getSignature().getSignature().toByteArray();
        Peer.Transaction tranWithOutSig = tran.toBuilder().clearSignature().build();
        PublicKey publicKey = CertUtil.genX509CertPrivateKey(
                new File("jks/jdk13/951002007l78123233.super_admin.jks"),
                "super_admin",
                "951002007l78123233.super_admin").getCertificate().getPublicKey();
        System.out.println(new ECDSASign("sha256withecdsa").verify(sigData, tranWithOutSig.toByteArray(), publicKey));
    }
}
