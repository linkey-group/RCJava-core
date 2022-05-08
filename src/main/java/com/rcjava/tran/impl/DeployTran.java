package com.rcjava.tran.impl;

import com.rcjava.protos.Peer.*;
import com.rcjava.tran.RCTran;
import com.rcjava.sign.TranSigner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.security.PrivateKey;
import java.util.UUID;

/**
 * @author zyf
 */
public class DeployTran implements RCTran {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private String txid;
    private String signAlgorithm;
    private CertId certId;
    private ChaincodeId chaincodeId;
    private ChaincodeDeploy.CodeType codeType;
    private String spcPackage;
    private String legal_prose;
    private int timeout;
    private int gasLimit;
    private String oid;
    private PrivateKey privateKey;

    private DeployTran(Builder builder) {
        txid = builder.txid;
        signAlgorithm = builder.signAlgorithm;
        certId = builder.certId;
        chaincodeId = builder.chaincodeId;
        codeType = builder.codeType;
        spcPackage = builder.spcPackage;
        legal_prose = builder.legal_prose;
        timeout = builder.timeout;
        gasLimit = builder.gasLimit;
        oid = builder.oid;
        privateKey = builder.privateKey;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(@Nonnull DeployTran copy) {
        Builder builder = new Builder();
        builder.txid = copy.getTxid();
        builder.signAlgorithm = copy.getSignAlgorithm();
        builder.certId = copy.getCertId();
        builder.chaincodeId = copy.getChaincodeId();
        builder.codeType = copy.getCodeType();
        builder.spcPackage = copy.getSpcPackage();
        builder.legal_prose = copy.getLegal_prose();
        builder.timeout = copy.getTimeout();
        builder.gasLimit = copy.getGasLimit();
        builder.oid = copy.getOid();
        builder.privateKey = copy.getPrivateKey();
        return builder;
    }

    public Builder toBuilder() {
        return new Builder()
                .setTxid(txid)
                .setSignAlgorithm(signAlgorithm)
                .setCertId(certId)
                .setChaincodeId(chaincodeId)
                .setCodeType(codeType)
                .setSpcPackage(spcPackage)
                .setLegal_prose(legal_prose)
                .setTimeout(timeout)
                .setGasLimit(gasLimit)
                .setOid(oid)
                .setPrivateKey(privateKey);
    }


    /**
     * 使用privateKey和相应算法进行签名
     *
     * @param privateKey
     * @param signAlgorithm
     * @return
     */
    @Override
    public Transaction getSignedTran(@Nonnull PrivateKey privateKey, @Nonnull String signAlgorithm) {
        String localTxid = UUID.randomUUID().toString().replace("-", "");
        ChaincodeDeploy chaincodeDeploy = ChaincodeDeploy.newBuilder()
                .setTimeout(timeout)
                .setCodePackage(spcPackage)
                .setLegalProse(legal_prose)
                .setCType(codeType)
                .build();
        Transaction tranDep = Transaction.newBuilder()
                .setId(txid == null ? localTxid : txid)
                .setType(Transaction.Type.CHAINCODE_DEPLOY)
                .setCid(chaincodeId)
                .setSpec(chaincodeDeploy)
                .setGasLimit(gasLimit)
                .setOid(oid)
                .build();
        return TranSigner.signTran(tranDep, certId, privateKey, signAlgorithm);
    }

    @Override
    public Transaction getSignedTran() {
        return this.getSignedTran(this.privateKey, this.signAlgorithm);
    }

    /**
     * 部分变量赋初值
     */
    public static final class Builder {
        private String txid;
        private String signAlgorithm;
        private CertId certId;
        private ChaincodeId chaincodeId;
        private ChaincodeDeploy.CodeType codeType;
        private String spcPackage;
        private String legal_prose = "";
        private int timeout = 500;
        private int gasLimit = 0;
        private String oid = "";
        private PrivateKey privateKey;

        private Builder() {
        }

        @Nonnull
        public Builder setTxid(@Nonnull String val) {
            txid = val;
            return this;
        }

        @Nonnull
        public Builder setSignAlgorithm(@Nonnull String val) {
            signAlgorithm = val;
            return this;
        }

        @Nonnull
        public Builder setCertId(@Nonnull CertId val) {
            certId = val;
            return this;
        }

        @Nonnull
        public Builder setChaincodeId(@Nonnull ChaincodeId val) {
            chaincodeId = val;
            return this;
        }

        @Nonnull
        public Builder setCodeType(@Nonnull ChaincodeDeploy.CodeType val) {
            codeType = val;
            return this;
        }

        @Nonnull
        public Builder setSpcPackage(@Nonnull String val) {
            spcPackage = val;
            return this;
        }

        @Nonnull
        public Builder setLegal_prose(@Nonnull String val) {
            legal_prose = val;
            return this;
        }

        @Nonnull
        public Builder setTimeout(int val) {
            timeout = val;
            return this;
        }

        @Nonnull
        public Builder setGasLimit(@Nonnull int val) {
            gasLimit = val;
            return this;
        }

        @Nonnull
        public Builder setOid(@Nonnull String val) {
            oid = val;
            return this;
        }

        @Nonnull
        public Builder setPrivateKey(@Nonnull PrivateKey val) {
            privateKey = val;
            return this;
        }

        @Nonnull
        public DeployTran build() {
            return new DeployTran(this);
        }
    }

    public String getTxid() {
        return txid;
    }

    public String getSignAlgorithm() {
        return signAlgorithm;
    }

    public CertId getCertId() {
        return certId;
    }

    public ChaincodeId getChaincodeId() {
        return chaincodeId;
    }

    public ChaincodeDeploy.CodeType getCodeType() {
        return codeType;
    }

    public String getSpcPackage() {
        return spcPackage;
    }

    public String getLegal_prose() {
        return legal_prose;
    }

    public int getTimeout() {
        return timeout;
    }

    public int getGasLimit() {
        return gasLimit;
    }

    public String getOid() {
        return oid;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }
}
