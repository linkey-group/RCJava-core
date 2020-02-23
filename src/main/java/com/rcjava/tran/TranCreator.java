package com.rcjava.tran;


import com.rcjava.protos.Peer.*;
import com.rcjava.sign.TranSigner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
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

    public Builder toBuilder() {
        return new Builder()
                .setPrivateKey(privateKey)
                .setSignAlgorithm(signAlgorithm);
    }

    /**
     * TODO 构造deploy交易
     *
     * @param tranId
     * @param certId
     * @param chaincodeId
     * @param spcPackage
     * @param legal_prose
     * @param timeout
     * @param ctype
     * @return
     * @throws Exception
     */
    public Transaction createDeployTran(String tranId, CertId certId, ChaincodeId chaincodeId, String spcPackage, String legal_prose, int timeout, ChaincodeDeploy.CodeType ctype) {
        if (null == tranId || "".equals(tranId) || "".equals(tranId.trim())) {
            tranId = UUID.randomUUID().toString().replace("-", "");
            logger.info("参数tranId为空，生成随机tranId：{}", tranId);
        }
        ChaincodeDeploy chaincodeDeploy = ChaincodeDeploy.newBuilder()
                .setTimeout(timeout)
                .setCodePackage(spcPackage)
                .setLegalProse(legal_prose)
                .setCtype(ctype)
                .build();
        Transaction tranDep = Transaction.newBuilder()
                .setId(tranId)
                .setType(Transaction.Type.CHAINCODE_DEPLOY)
                .setCid(chaincodeId)
                .setSpec(chaincodeDeploy)
                .build();
        return TranSigner.signTran(tranDep, certId, privateKey, signAlgorithm);
    }

    /**
     * TODO 构造invoke交易
     *
     * @param tranId
     * @param certId
     * @param chaincodeId
     * @param chaincodeInputFunc
     * @param params
     * @return
     */
    public Transaction createInvokeTran(String tranId, CertId certId, ChaincodeId chaincodeId, String chaincodeInputFunc, List<String> params) {
        if (null == tranId || "".equals(tranId) || "".equals(tranId.trim())) {
            tranId = UUID.randomUUID().toString().replace("-", "");
            logger.info("参数tranId为空，生成随机tranId：{}", tranId);
        }
        ChaincodeInput ipt = ChaincodeInput.newBuilder()
                .setFunction(chaincodeInputFunc)
                .addAllArgs(params).build();
        Transaction tranInv = Transaction.newBuilder()
                .setId(tranId)
                .setType(Transaction.Type.CHAINCODE_INVOKE)
                .setCid(chaincodeId)
                .setIpt(ipt)
                .build();
        return TranSigner.signTran(tranInv, certId, privateKey, signAlgorithm);
    }


    /**
     * TODO 构造改变合约状态的交易
     *
     * @param tranId
     * @param certId
     * @param chaincodeId
     * @param state
     * @return
     * @throws Exception
     */
    public Transaction createCidStateTran(String tranId, CertId certId, ChaincodeId chaincodeId, Boolean state) throws Exception {
        if (null == tranId || "".equals(tranId) || "".equals(tranId.trim())) {
            tranId = UUID.randomUUID().toString().replace("-", "");
            logger.info("参数tranId为空，生成随机tranId：{}", tranId);
        }
        Transaction tranSt = Transaction.newBuilder()
                .setId(tranId)
                .setType(Transaction.Type.CHAINCODE_SET_STATE)
                .setCid(chaincodeId)
                .setState(state)
                .build();
        return TranSigner.signTran(tranSt, certId, privateKey, signAlgorithm);
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
