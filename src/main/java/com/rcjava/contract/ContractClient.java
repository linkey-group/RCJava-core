package com.rcjava.contract;


import com.alibaba.fastjson.JSONObject;
import com.rcjava.client.TranPostClient;
import com.rcjava.protos.Peer;
import com.rcjava.protos.Peer.CertId;
import com.rcjava.protos.Peer.ChaincodeId;
import com.rcjava.protos.Peer.ChaincodeInput;
import com.rcjava.protos.Peer.Transaction;
import com.rcjava.tran.TranCreator;
import com.rcjava.tran.impl.CidStateTran;
import com.rcjava.tran.impl.DeployTran;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

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
    private ChaincodeId chaincodeId;
    private ContractUser contractUser;

    private CertId certId;
    private TranCreator tranCreator;
    private TranPostClient tranPostClient;


    public ContractClient(String host, ContractUser contractUser) {
        this.host = host;
        this.contractUser = contractUser;
        this.certId = contractUser.getCertId();
        this.tranCreator = new TranCreator(contractUser.getPrivateKey(), contractUser.getSignAlgorithm());
        this.tranPostClient = new TranPostClient(host);
    }

    public ContractClient(String host, ChaincodeId chaincodeId, ContractUser contractUser) {
        this.host = host;
        this.chaincodeId = chaincodeId;
        this.contractUser = contractUser;
        this.certId = contractUser.getCertId();
        this.tranCreator = new TranCreator(contractUser.getPrivateKey(), contractUser.getSignAlgorithm());
        this.tranPostClient = new TranPostClient(host);

    }

    /**
     * 部署合约
     *
     * @param contractCode 合约代码
     */
    public JSONObject deployContract(String contractCode) {
        JSONObject deployRes = this.deployContract(requireNonNull(this.chaincodeId, "ChaincodeId不能为空"), contractCode);
        return deployRes;
    }

    /**
     * 部署合约
     *
     * @param chaincodeId  链码
     * @param contractCode 合约代码
     */
    public JSONObject deployContract(ChaincodeId chaincodeId, String contractCode) {
        DeployTran deployTran = DeployTran.newBuilder()
                .setTxid(DigestUtils.sha256Hex(contractCode))
                .setCertId(this.certId)
                .setChaincodeId(requireNonNull(chaincodeId, "ChaincodeId不能为空"))
                .setSpcPackage(contractCode)
                .setLegal_prose("")
                .setTimeout(5000)
                .setCodeType(Peer.ChaincodeDeploy.CodeType.CODE_SCALA)
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
        JSONObject setStateRes = this.setContractState(tranId, requireNonNull(this.chaincodeId, "ChaincodeId不能为空"), state);
        return setStateRes;
    }

    /**
     * 设置合约状态，比如设置为false，就禁用合约
     *
     * @param tranId      用户自定义交易ID
     * @param chaincodeId 链码
     * @param state       合约状态
     */
    public JSONObject setContractState(String tranId, ChaincodeId chaincodeId, boolean state) {
        CidStateTran cidStateTran = CidStateTran.newBuilder()
                .setTxid(tranId)
                .setCertId(this.certId)
                .setChaincodeId(requireNonNull(chaincodeId, "ChaincodeId不能为空"))
                .setState(state)
                .setPrivateKey(this.contractUser.getPrivateKey())
                .setSignAlgorithm(this.contractUser.getSignAlgorithm())
                .build();
        Transaction signedCidStateTran = cidStateTran.getSignedTran();
        JSONObject setStateRes = tranPostClient.postSignedTran(signedCidStateTran);
        return setStateRes;
    }

    /**
     * 升级合约
     *
     * @param contractCode 合约代码
     */
    public JSONObject updateContractVersion(String contractCode) {
        JSONObject updateRes = this.deployContract(contractCode);
        return updateRes;
    }

    /**
     * 升级合约
     *
     * @param chaincodeId  链码
     * @param contractCode 合约代码
     */
    public JSONObject updateContractVersion(ChaincodeId chaincodeId, String contractCode) {
        JSONObject updateRes = this.deployContract(requireNonNull(chaincodeId, "ChaincodeId不能为空"), contractCode);
        return updateRes;
    }

    /**
     * 调用合约
     *
     * @param chaincodeInput 合约中的函数名(方法名) + 参数
     */
    public JSONObject invokeContract(ChaincodeInput chaincodeInput) {
        String tranId = UUID.randomUUID().toString().replace("-", "");
        Transaction signedInvokeTran = this.tranCreator.createInvokeTran(tranId, this.certId, requireNonNull(this.chaincodeId, "ChaincodeId不能为空"), chaincodeInput);
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
        JSONObject invokeRes = this.invokeContract(tranId, requireNonNull(this.chaincodeId, "ChaincodeId不能为空"), function, args);
        return invokeRes;
    }

    /**
     * 调用合约
     *
     * @param tranId   用户自定义交易ID
     * @param function 合约中的函数名(方法名)
     * @param args     方法参数
     */
    public JSONObject invokeContract(String tranId, ChaincodeId chaincodeId, String function, String args) {
        Transaction signedInvokeTran = tranCreator.createInvokeTran(tranId, this.certId, requireNonNull(chaincodeId, "ChaincodeId不能为空"), function, args);
        String tranHex = Hex.encodeHexString(signedInvokeTran.toByteArray());
        JSONObject invokeRes = tranPostClient.postSignedTran(tranHex);
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
}
