package com.rcjava.contract;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.rcjava.model.Transfer;
import com.rcjava.protos.Peer;
import com.rcjava.util.CertUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.security.PrivateKey;

/**
 * @author zyf
 */
public class ContractClientTest {

    private String host = "localhost:9081";
    private Transfer transfer = new Transfer("121000005l35120456", "12110107bi45jh675g", 5);

    @Test
    @DisplayName("调用合约，执行转账函数")
    public void testInvokeContract() {
        // 首先是操作合约的用户
        Peer.CertId certId = Peer.CertId.newBuilder().setCreditCode("121000005l35120456").setCertName("node1").build();
        PrivateKey privateKey = CertUtil.genX509CertPrivateKey(
                new File("jks/jdk13/121000005l35120456.node1.jks"),
                "123",
                "121000005l35120456.node1").getPrivateKey();
        ContractUser user = new ContractUser(certId, privateKey);
        // 确定合约ID
        Peer.ChaincodeId contractAssetsId = Peer.ChaincodeId.newBuilder().setChaincodeName("ContractAssetsTPL").setVersion(1).build();
        // 构造client
        ContractClient client = new ContractClient(host, contractAssetsId, user);
        // 调用上一步标识好的合约中的transfer方法，并给定参数
        JSONObject res = client.invokeContract("transfer", JSON.toJSONString(transfer));
        System.out.println(res);
    }

    @Test
    @DisplayName("调用合约，执行转账函数")
    public void testInvokeContract_1() {
        // 首先是操作合约的用户
        Peer.CertId certId = Peer.CertId.newBuilder().setCreditCode("121000005l35120456").setCertName("node1").build();
        PrivateKey privateKey = CertUtil.genX509CertPrivateKey(
                new File("jks/jdk13/121000005l35120456.node1.jks"),
                "123",
                "121000005l35120456.node1").getPrivateKey();
        ContractUser user = new ContractUser(certId, privateKey);
        // 确定合约ID
        Peer.ChaincodeId contractAssetsId = Peer.ChaincodeId.newBuilder().setChaincodeName("ContractAssetsTPL").setVersion(1).build();
        // 构造client
        ContractClient client = new ContractClient(host, contractAssetsId, user);
        // 方法+参数
        Peer.ChaincodeInput input = Peer.ChaincodeInput.newBuilder().setFunction("transfer").addArgs(JSON.toJSONString(transfer)).build();
        // 调用上一步标识好的合约中的transfer方法，并给定参数
        JSONObject res = client.invokeContract(input);
        System.out.println(res);
    }
}
