package com.rcjava.tran;


import com.rcjava.protos.Peer.*;
import com.rcjava.sign.RCTranSigner;
import com.rcjava.sign.TranSigner;
import com.rcjava.tran.impl.CidStateTran;
import com.rcjava.tran.impl.DeployTran;
import com.rcjava.tran.impl.InvokeTran;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.security.PrivateKey;
import java.util.List;
import java.util.UUID;

/**
 * @author zyf
 */
public class TranCreator {

    private static Logger logger = LoggerFactory.getLogger(TranCreator.class);

    private PrivateKey privateKey = null;
    private String signAlgorithm = null;

    public TranCreator() {
    }

    public TranCreator(PrivateKey privateKey, String signAlgorithm) {
        this.privateKey = privateKey;
        this.signAlgorithm = signAlgorithm;
    }

    private TranCreator(Builder builder) {
        setPrivateKey(builder.privateKey);
        setSignAlgorithm(builder.signAlgorithm);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static TranCreator.Builder newBuilder(@Nonnull TranCreator copy) {
        Builder builder = new Builder();
        builder.signAlgorithm = copy.getSignAlgorithm();
        builder.privateKey = copy.getPrivateKey();
        return builder;
    }

    public Builder toBuilder() {
        return new Builder()
                .setPrivateKey(privateKey)
                .setSignAlgorithm(signAlgorithm);
    }

    /**
     * @param certId          用户证书标识
     * @param chaincodeId     合约ID
     * @param chaincodeDeploy 部署合约时的一些描述信息
     * @param gasLimit        可选，如果有设置按照预设的资源消耗,超出则终止执行；否则不限制
     * @param oid             重放举证：交易实例id, 可选, 导出实例时，要求提供同一区块内部，同一合约实例的交易顺序及证明， 默认空为全局的实例id
     * @return
     */
    public Transaction createDeployTran(@Nonnull CertId certId, @Nonnull ChaincodeId chaincodeId, @Nonnull ChaincodeDeploy chaincodeDeploy,
                                        @Nonnull int gasLimit, @Nonnull String oid) {
        String tranId = UUID.randomUUID().toString().replace("-", "");
        return createDeployTran(tranId, certId, chaincodeId, chaincodeDeploy, gasLimit, oid);
    }

    /**
     * @param tranId          交易ID
     * @param certId          用户证书标识
     * @param chaincodeId     合约ID
     * @param chaincodeDeploy 部署合约时的一些描述信息
     * @param gasLimit        可选，如果有设置按照预设的资源消耗,超出则终止执行；否则不限制
     * @param oid             重放举证：交易实例id, 可选, 导出实例时，要求提供同一区块内部，同一合约实例的交易顺序及证明， 默认空为全局的实例id
     * @return
     */
    public Transaction createDeployTran(@Nullable String tranId, @Nonnull CertId certId, @Nonnull ChaincodeId chaincodeId, @Nonnull ChaincodeDeploy chaincodeDeploy,
                                        @Nonnull int gasLimit, @Nonnull String oid) {
        if (null == tranId || "".equals(tranId) || "".equals(tranId.trim())) {
            tranId = UUID.randomUUID().toString().replace("-", "");
            logger.info("参数tranId为空，生成随机tranId：{}", tranId);
        }
        Transaction tranDep = Transaction.newBuilder()
                .setId(tranId)
                .setType(Transaction.Type.CHAINCODE_DEPLOY)
                .setCid(chaincodeId)
                .setSpec(chaincodeDeploy)
                .setGasLimit(gasLimit)
                .setOid(oid)
                .build();
        return TranSigner.signTran(tranDep, certId, privateKey, signAlgorithm);
    }

    /**
     * @param deployTran
     * @return
     */
    public Transaction createDeployTran(@Nonnull DeployTran deployTran) {
        logger.info("指定tranId：{}", deployTran.getTxid());
        return RCTranSigner.getSignedTran(deployTran, privateKey, signAlgorithm);
    }

    /**
     * @param tranId      交易ID
     * @param certId      用户证书标识
     * @param chaincodeId 合约ID
     * @param function    所调用的合约方法
     * @param params      传递给合约方法的实参
     * @param gasLimit    可选，如果有设置按照预设的资源消耗,超出则终止执行；否则不限制
     * @param oid         重放举证：交易实例id, 可选, 导出实例时，要求提供同一区块内部，同一合约实例的交易顺序及证明， 默认空为全局的实例id
     * @return
     */
    public Transaction createInvokeTran(@Nullable String tranId, @Nonnull CertId certId, @Nonnull ChaincodeId chaincodeId, @Nonnull String function, @Nonnull List<String> params,
                                        @Nonnull int gasLimit, @Nonnull String oid) {
        ChaincodeInput ipt = ChaincodeInput.newBuilder()
                .setFunction(function)
                .addAllArgs(params).build();
        return createInvokeTran(tranId, certId, chaincodeId, ipt, gasLimit, oid);
    }

    /**
     * @param tranId      交易ID
     * @param certId      用户证书标识
     * @param chaincodeId 合约ID
     * @param function    所调用的合约方法
     * @param param       传递给合约方法的实参
     * @param gasLimit    可选，如果有设置按照预设的资源消耗,超出则终止执行；否则不限制
     * @param oid         重放举证：交易实例id, 可选, 导出实例时，要求提供同一区块内部，同一合约实例的交易顺序及证明， 默认空为全局的实例id
     * @return
     */
    public Transaction createInvokeTran(@Nullable String tranId, @Nonnull CertId certId, @Nonnull ChaincodeId chaincodeId, @Nonnull String function, @Nonnull String param,
                                        @Nonnull int gasLimit, @Nonnull String oid) {
        ChaincodeInput ipt = ChaincodeInput.newBuilder()
                .setFunction(function)
                .addArgs(param).build();
        return createInvokeTran(tranId, certId, chaincodeId, ipt, gasLimit, oid);
    }

    /**
     * 使用用户指定的id
     *
     * @param tranId      交易ID
     * @param certId      用户证书标识
     * @param chaincodeId 合约ID
     * @param ipt         要调用的合约方法和参数描述
     * @param gasLimit    可选，如果有设置按照预设的资源消耗,超出则终止执行；否则不限制
     * @param oid         重放举证：交易实例id, 可选, 导出实例时，要求提供同一区块内部，同一合约实例的交易顺序及证明， 默认空为全局的实例id
     * @return
     */
    public Transaction createInvokeTran(@Nonnull String tranId, @Nonnull CertId certId, @Nonnull ChaincodeId chaincodeId, @Nonnull ChaincodeInput ipt,
                                        @Nonnull int gasLimit, @Nonnull String oid) {
        logger.info("指定tranId：{}", tranId);
        Transaction tranInv = Transaction.newBuilder()
                .setId(tranId)
                .setType(Transaction.Type.CHAINCODE_INVOKE)
                .setCid(chaincodeId)
                .setIpt(ipt)
                .setGasLimit(gasLimit)
                .setOid(oid)
                .build();
        return TranSigner.signTran(tranInv, certId, privateKey, signAlgorithm);
    }

    /**
     * 默认使用java-uuid
     *
     * @param certId      用户证书标识
     * @param chaincodeId 合约ID
     * @param ipt         要调用的合约方法和参数描述
     * @param gasLimit    可选，如果有设置按照预设的资源消耗,超出则终止执行；否则不限制
     * @param oid         重放举证：交易实例id, 可选, 导出实例时，要求提供同一区块内部，同一合约实例的交易顺序及证明， 默认空为全局的实例id
     * @return
     */
    public Transaction createInvokeTran(@Nonnull CertId certId, @Nonnull ChaincodeId chaincodeId, @Nonnull ChaincodeInput ipt,
                                        @Nonnull int gasLimit, @Nonnull String oid) {
        String tranId = UUID.randomUUID().toString().replace("-", "");
        logger.info("生成随机tranId：{}", tranId);
        return createInvokeTran(tranId, certId, chaincodeId, ipt, gasLimit, oid);
    }

    /**
     * @param invokeTran
     * @return
     */
    public Transaction createInvokeTran(@Nonnull InvokeTran invokeTran) {
        logger.info("指定tranId：{}", invokeTran.getTxid());
        return RCTranSigner.getSignedTran(invokeTran, privateKey, signAlgorithm);
    }


    /**
     * @param tranId      交易ID
     * @param certId      用户证书标识
     * @param chaincodeId 合约ID
     * @param state       合约状态
     * @param gasLimit    如果有设置按照预设的资源消耗,超出则终止执行；否则不限制
     * @param oid         重放举证：交易实例id, 可选, 导出实例时，要求提供同一区块内部，同一合约实例的交易顺序及证明， 默认空为全局的实例id
     * @return
     * @throws Exception
     */
    public Transaction createCidStateTran(@Nullable String tranId, @Nonnull CertId certId, @Nonnull ChaincodeId chaincodeId, @Nonnull Boolean state,
                                          @Nonnull int gasLimit, @Nonnull String oid) throws Exception {
        if (null == tranId || "".equals(tranId) || "".equals(tranId.trim())) {
            tranId = UUID.randomUUID().toString().replace("-", "");
            logger.info("参数tranId为空，生成随机tranId：{}", tranId);
        }
        Transaction tranSt = Transaction.newBuilder()
                .setId(tranId)
                .setType(Transaction.Type.CHAINCODE_SET_STATE)
                .setCid(chaincodeId)
                .setState(state)
                .setGasLimit(gasLimit)
                .setOid(oid)
                .build();
        return TranSigner.signTran(tranSt, certId, privateKey, signAlgorithm);
    }

    /**
     * @param certId      用户证书标识
     * @param chaincodeId 合约ID
     * @param state       合约状态
     * @param gasLimit    如果有设置按照预设的资源消耗,超出则终止执行；否则不限制
     * @param oid         重放举证：交易实例id, 可选, 导出实例时，要求提供同一区块内部，同一合约实例的交易顺序及证明， 默认空为全局的实例id
     * @return
     * @throws Exception
     */
    public Transaction createCidStateTran(@Nonnull CertId certId, @Nonnull ChaincodeId chaincodeId, @Nonnull Boolean state,
                                          @Nonnull int gasLimit, @Nonnull String oid) throws Exception {
        String tranId = UUID.randomUUID().toString().replace("-", "");
        logger.info("生成随机tranId：{}", tranId);
        return createCidStateTran(tranId, certId, chaincodeId, state, gasLimit, oid);
    }

    /**
     * @param cidStateTran
     * @return
     */
    public Transaction createCidStateTran(@Nonnull CidStateTran cidStateTran) {
        logger.info("指定tranId：{}", cidStateTran.getTxid());
        return RCTranSigner.getSignedTran(cidStateTran, privateKey, signAlgorithm);
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public String getSignAlgorithm() {
        return signAlgorithm;
    }

    public void setSignAlgorithm(String signAlgorithm) {
        this.signAlgorithm = signAlgorithm;
    }

    public static final class Builder {

        private PrivateKey privateKey;
        private String signAlgorithm;

        private Builder() {
        }

        @Nonnull
        public Builder setPrivateKey(@Nonnull PrivateKey privateKey) {
            this.privateKey = privateKey;
            return this;
        }

        @Nonnull
        public Builder setSignAlgorithm(@Nonnull String signAlgorithm) {
            this.signAlgorithm = signAlgorithm;
            return this;
        }

        @Nonnull
        public TranCreator build() {
            return new TranCreator(this);
        }
    }


}
