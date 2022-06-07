package com.rcjava.tran.impl;

import com.alibaba.fastjson2.JSON;
import com.rcjava.model.Transfer;
import com.rcjava.protos.Peer;
import com.rcjava.sign.RCTranSigner;
import com.rcjava.util.CertUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.security.PrivateKey;

import static com.google.common.truth.Truth.*;

/**
 * @author zyf
 */
public class InvokeTranTest {

    private Peer.CertId certId = Peer.CertId.newBuilder().setCreditCode("121000005l35120456").setCertName("node1").build(); // 签名ID

    //这个是给转账交易示范用的，此ID需要与repchain合约部署的一致
    private Peer.ChaincodeId contractAssetsId = Peer.ChaincodeId.newBuilder().setChaincodeName("ContractAssetsTPL").setVersion(1).build();

    private PrivateKey privateKey = CertUtil.genX509CertPrivateKey(
            new File("jks/121000005l35120456.node1.jks"),
            "123",
            "121000005l35120456.node1").getPrivateKey();

    private Transfer transfer = new Transfer("121000005l35120456", "12110107bi45jh675g", 5);

    @Test
    @DisplayName("测试使用InvokeTran来构造invoke交易")
    void testGenerateTran() {

        InvokeTran.Builder builder = InvokeTran.newBuilder();
        Peer.ChaincodeInput chaincodeInput = Peer.ChaincodeInput.newBuilder()
                .setFunction("transfer")
                .addArgs(JSON.toJSONString(transfer))
                .build();
        // 交易的签名算法根据对应RepChain版本进行设置
        InvokeTran invokeTran = builder
                .setChaincodeInput(chaincodeInput)
                .setCertId(certId)
                .setChaincodeId(contractAssetsId)
                .setPrivateKey(privateKey)
                .setSignAlgorithm("SHA256withECDSA")
                .build();

        assertThat(invokeTran.getTxid()).isNull();

        Peer.Transaction transaction = invokeTran.getSignedTran();
        Peer.Transaction transaction_1 = invokeTran.getSignedTran();
        Peer.Transaction transaction_2 = invokeTran.getSignedTran(privateKey, "sha256withecdsa");
        Peer.Transaction transaction_3 = invokeTran.toBuilder().setPrivateKey(privateKey).setSignAlgorithm("sha256withecdsa").build().getSignedTran();
        Peer.Transaction transaction_4 = RCTranSigner.getSignedTran(invokeTran, privateKey, "sha256withecdsa");

        assertThat(transaction_1.getSignature().getTmLocal().getNanos())
                .isNotEqualTo(transaction.getSignature().getTmLocal().getNanos());
        assertThat(transaction_1.getId()).isNotEqualTo(transaction.getId());
        assertThat(transaction_2.getId()).isNotEqualTo(transaction_1.getId());
        assertThat(transaction_3.getId()).isNotEqualTo(transaction_2.getId());
        assertThat(transaction_4.getId()).isNotEqualTo(transaction_3.getId());

        InvokeTran invokeTran_1 = invokeTran.toBuilder().setTxid("123456789").build();
        assertThat(invokeTran_1.getTxid()).isEqualTo("123456789");
        assertThat(invokeTran_1.getSignedTran().getId()).isEqualTo("123456789");

    }
}
