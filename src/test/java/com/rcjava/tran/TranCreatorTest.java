package com.rcjava.tran;

import com.alibaba.fastjson.JSON;
import com.rcjava.model.Transfer;
import com.rcjava.protos.Peer;
import com.rcjava.sign.RCTranSigner;
import com.rcjava.tran.impl.InvokeTran;
import com.rcjava.util.CertUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.security.PrivateKey;
import java.util.Objects;
import java.util.UUID;

import static com.google.common.truth.Truth.assertThat;

/**
 * @author zyf
 */
public class TranCreatorTest {

    private Peer.CertId certId = Peer.CertId.newBuilder().setCreditCode("121000005l35120456").setCertName("node1").build(); // 签名ID

    //这个是给转账交易示范用的，此ID需要与repchain合约部署的一致
    private Peer.ChaincodeId contractAssetsId = Peer.ChaincodeId.newBuilder().setChaincodeName("ContractAssetsTPL").setVersion(1).build();

    private PrivateKey privateKey = Objects.requireNonNull(CertUtil.genX509CertPrivateKey(
            new File("jks/121000005l35120456.node1.jks"),
            "123",
            "121000005l35120456.node1"), "获取证书和私钥失败").getPrivateKey();

    private String tranId = UUID.randomUUID().toString().replace("-", "");

    private Transfer transfer = new Transfer("121000005l35120456", "12110107bi45jh675g", 5);


    @Test
    @DisplayName("test TranCreator, test Builder、toBuilder and methods")
    void testCreateInvokeTran() {

        // Builder的测试
        TranCreator tranCreator = TranCreator.newBuilder().setPrivateKey(privateKey).setSignAlgorithm("sha1withecdsa").build();
        Peer.Transaction tran = tranCreator.createInvokeTran(tranId, certId, contractAssetsId, "transfer", JSON.toJSONString(transfer));

        assertThat(tran.getId()).isEqualTo(tranId);
        assertThat(tran.getSignature().getCertId().getCertName()).isEqualTo("node1");

        Peer.ChaincodeInput chaincodeInput = Peer.ChaincodeInput.newBuilder().setFunction("transfer").addArgs(JSON.toJSONString(transfer)).build();

        // toBuilder的测试
        TranCreator tranCreator_1 = tranCreator.toBuilder().setSignAlgorithm("sha256withecdsa").build();
        Peer.Transaction tran_1 = tranCreator_1.createInvokeTran(tranId, certId, contractAssetsId, chaincodeInput);

        assertThat(tran_1.getId()).isEqualTo(tranId);
        // toBuilder测试的断言
        assertThat(tranCreator_1.getSignAlgorithm()).isEqualTo("sha256withecdsa");

    }

    @Test
    @DisplayName("test TranCreator, create tran from rctran")
    void testCreateTranFromRCTran() {

        Peer.ChaincodeInput chaincodeInput = Peer.ChaincodeInput.newBuilder().setFunction("transfer").addArgs(JSON.toJSONString(transfer)).build();
        InvokeTran invokeTran = InvokeTran.newBuilder()
                .setTxid(tranId)
                .setChaincodeId(contractAssetsId)
                .setChaincodeInput(chaincodeInput)
                .setCertId(certId)
                .build();
        TranCreator tranCreator = TranCreator.newBuilder().setPrivateKey(privateKey).setSignAlgorithm("sha1withecdsa").build();

        Peer.Transaction invoke_1 = tranCreator.createInvokeTran(invokeTran);

        Peer.Transaction invoke_2 = invokeTran.getSignedTran(privateKey, "sha1withecdsa");

        Peer.Transaction invoke_3 = invokeTran.toBuilder().setPrivateKey(privateKey).setSignAlgorithm("sha1withecdsa").build().getSignedTran();

        Peer.Transaction invoke_4 = RCTranSigner.getSignedTran(invokeTran, privateKey, "sha1withecdsa");

        assertThat(invoke_1.getId()).isEqualTo(invoke_2.getId());
        assertThat(invoke_2.getId()).isEqualTo(invoke_3.getId());
        assertThat(invoke_3.getId()).isEqualTo(invoke_4.getId());
    }
}
