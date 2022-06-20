package com.rcjava.contract;


import com.alibaba.fastjson2.JSONObject;
import com.rcjava.client.TranPostClient;
import com.rcjava.protos.Peer;
import com.rcjava.protos.Peer.CertId;
import com.rcjava.protos.Peer.ChaincodeId;
import com.rcjava.protos.Peer.ChaincodeInput;
import com.rcjava.protos.Peer.ChaincodeDeploy;
import com.rcjava.protos.Peer.Transaction;
import com.rcjava.tran.TranCreator;
import com.rcjava.tran.impl.CidStateTran;
import com.rcjava.tran.impl.DeployTran;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import javax.net.ssl.SSLContext;
import java.util.Objects;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

/**
 * 部署升级合约、调用合约、修改合约状态客户端
 * 面向RepChainContract编程，而不是面相RepChainAPI接口编程，^_^
 *
 * @author zyf
 */
public class ContractClient {

    private String host;
    private SSLContext sslContext;
    private ChaincodeId chaincodeId;
    private ContractUser contractUser;

    private CertId certId;
    private TranCreator tranCreator;
    private TranPostClient tranPostClient;

    private int gasLimit = 0;
    private String oid = "";

    public ContractClient(String host, ChaincodeId chaincodeId, ContractUser contractUser) {
        this.host = host;
        this.chaincodeId = chaincodeId;
        this.contractUser = contractUser;
        this.certId = contractUser.getCertId();
        this.tranCreator = new TranCreator(contractUser.getPrivateKey(), contractUser.getSignAlgorithm());
        this.tranPostClient = new TranPostClient(host);

    }

    public ContractClient(String host, ChaincodeId chaincodeId, ContractUser contractUser, SSLContext sslContext) {
        this.host = host;
        this.sslContext = sslContext;
        this.chaincodeId = chaincodeId;
        this.contractUser = contractUser;
        this.certId = contractUser.getCertId();
        this.tranCreator = new TranCreator(contractUser.getPrivateKey(), contractUser.getSignAlgorithm());
        this.tranPostClient = new TranPostClient(host, sslContext);

    }

    /**
     * 部署合约, 默认: CODE_SCALA RUN_SERIAL STATE_BLOCK CONTRACT_CUSTOM
     *
     * @param contractCode 合约代码
     */
    public JSONObject deployContract(String contractCode) {
        ChaincodeDeploy chaincodeDeploy = ChaincodeDeploy.newBuilder()
                .setTimeout(5000)
                .setCodePackage(contractCode)
                .setLegalProse("")
                .setCType(ChaincodeDeploy.CodeType.CODE_SCALA)
                .setRType(ChaincodeDeploy.RunType.RUN_SERIAL)
                .setSType(ChaincodeDeploy.StateType.STATE_BLOCK)
                .setInitParameter("")
                .setCclassification(ChaincodeDeploy.ContractClassification.CONTRACT_CUSTOM)
                .build();
        JSONObject deployRes = deployContract(chaincodeDeploy);
        return deployRes;
    }

    /**
     * 部署合约
     *
     * @param chaincodeDeploy 合约代码信息
     */
    public JSONObject deployContract(ChaincodeDeploy chaincodeDeploy) {
        DeployTran deployTran = DeployTran.newBuilder()
                .setTxid(DigestUtils.sha256Hex(chaincodeDeploy.getCodePackage()))
                .setCertId(certId)
                .setChaincodeId(requireNonNull(chaincodeId, "ChaincodeId不能为空"))
                .setChaincodeDeploy(chaincodeDeploy)
                .setGasLimit(gasLimit)
                .setOid(oid)
                .build();
        Peer.Transaction signedDeployTran = this.tranCreator.createDeployTran(deployTran);
        JSONObject deployRes = tranPostClient.postSignedTran(signedDeployTran);
        return deployRes;
    }


    /**
     * 设置合约状态，比如设置为false，就禁用合约
     *
     * @param state 合约状态
     */
    public JSONObject setContractState(boolean state) {
        String tranId = UUID.randomUUID().toString().replace("-", "");
        JSONObject setStateRes = this.setContractState(tranId, state);
        return setStateRes;
    }

    /**
     * 设置合约状态，比如设置为false，就禁用合约
     *
     * @param tranId 用户自定义交易ID
     * @param state  合约状态
     */
    public JSONObject setContractState(String tranId, boolean state) {
        CidStateTran cidStateTran = CidStateTran.newBuilder()
                .setTxid(tranId)
                .setCertId(this.certId)
                .setChaincodeId(requireNonNull(chaincodeId, "ChaincodeId不能为空"))
                .setState(state)
                .setPrivateKey(this.contractUser.getPrivateKey())
                .setSignAlgorithm(this.contractUser.getSignAlgorithm())
                .setGasLimit(gasLimit)
                .setOid(oid)
                .build();
        Transaction signedCidStateTran = cidStateTran.getSignedTran();
        JSONObject setStateRes = tranPostClient.postSignedTran(signedCidStateTran);
        return setStateRes;
    }

    /**
     * 升级合约
     *
     * @param version      新的合约对应的版本号
     * @param contractCode 合约代码
     */
    public JSONObject updateContractVersion(int version, String contractCode) {
        ContractClient updateClient = Objects.isNull(sslContext)
                ? new ContractClient(host, chaincodeId.toBuilder().setVersion(version).build(), contractUser)
                : new ContractClient(host, chaincodeId.toBuilder().setVersion(version).build(), contractUser, sslContext);
        JSONObject updateRes = updateClient.deployContract(contractCode);
        return updateRes;
    }

    /**
     * 升级合约
     *
     * @param version         新的合约对应的版本号
     * @param chaincodeDeploy 合约代码信息
     */
    public JSONObject updateContractVersion(int version, ChaincodeDeploy chaincodeDeploy) {
        ContractClient updateClient = Objects.isNull(sslContext)
                ? new ContractClient(host, chaincodeId.toBuilder().setVersion(version).build(), contractUser)
                : new ContractClient(host, chaincodeId.toBuilder().setVersion(version).build(), contractUser, sslContext);
        JSONObject updateRes = updateClient.deployContract(chaincodeDeploy);
        return updateRes;
    }

    /**
     * 调用合约
     *
     * @param chaincodeInput 合约中的函数名(方法名) + 参数
     */
    public JSONObject invokeContract(ChaincodeInput chaincodeInput) {
        String tranId = UUID.randomUUID().toString().replace("-", "");
        JSONObject invokeRes = this.invokeContract(tranId, chaincodeInput);
        return invokeRes;
    }

    /**
     * 调用合约
     *
     * @param tranId         用户自定义交易ID
     * @param chaincodeInput 合约中的函数名(方法名) + 参数
     */
    public JSONObject invokeContract(String tranId, ChaincodeInput chaincodeInput) {
        Transaction signedInvokeTran = this.tranCreator.createInvokeTran(tranId, this.certId, requireNonNull(this.chaincodeId, "ChaincodeId不能为空"), chaincodeInput, gasLimit, oid);
        String tranHex = Hex.encodeHexString(signedInvokeTran.toByteArray());
        JSONObject invokeRes = tranPostClient.postSignedTran(tranHex);
        return invokeRes;
    }

    /**
     * 调用合约
     *
     * @param function 合约中的函数名(方法名)
     * @param args     方法参数
     */
    public JSONObject invokeContract(String function, String args) {
        String tranId = UUID.randomUUID().toString().replace("-", "");
        JSONObject invokeRes = this.invokeContract(tranId, function, args);
        return invokeRes;
    }

    /**
     * 调用合约
     *
     * @param tranId   用户自定义交易ID
     * @param function 合约中的函数名(方法名)
     * @param args     方法参数
     */
    public JSONObject invokeContract(String tranId, String function, String args) {
        ChaincodeInput chaincodeInput = ChaincodeInput.newBuilder().setFunction(function).addArgs(args).build();
        JSONObject invokeRes = this.invokeContract(tranId, chaincodeInput);
        return invokeRes;
    }

    /**
     * 对chainCodeId做判空
     *
     * @param chaincodeId
     */
    private void checkNull(ChaincodeId chaincodeId) {
        if (Objects.isNull(chaincodeId)) {
            throw new RuntimeException("ChaincodeId不能为空");
        }
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public ChaincodeId getChaincodeId() {
        return chaincodeId;
    }

    public void setChaincodeId(ChaincodeId chaincodeId) {
        this.chaincodeId = chaincodeId;
    }

    public ContractUser getContractUser() {
        return contractUser;
    }

    public void setContractUser(ContractUser contractUser) {
        this.contractUser = contractUser;
    }

    public CertId getCertId() {
        return certId;
    }

    public void setCertId(CertId certId) {
        this.certId = certId;
    }

    public int getGasLimit() {
        return gasLimit;
    }

    public void setGasLimit(int gasLimit) {
        this.gasLimit = gasLimit;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }
}
