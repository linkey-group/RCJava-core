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
        privateKey = builder.privateKey;
    }

    public static Builder newBuilder() {
        return new Builder();
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
                .setCtype(codeType)
                .build();
        Transaction tranDep = Transaction.newBuilder()
                .setId(txid == null ? localTxid : txid)
                .setType(Transaction.Type.CHAINCODE_DEPLOY)
                .setCid(chaincodeId)
                .setSpec(chaincodeDeploy)
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
        private String txid = null;
        private String signAlgorithm = null;
        private CertId certId;
        private ChaincodeId chaincodeId;
        private ChaincodeDeploy.CodeType codeType;
        private String spcPackage;
        private String legal_prose = "";
        private int timeout = 500;
        private PrivateKey privateKey = null;

        private Builder() {
        }

        @Nonnull
        public Builder setTxid(@Nonnull String txid) {
            this.txid = txid;
            return this;
        }

        @Nonnull
        public Builder setSignAlgorithm(@Nonnull String signAlgorithm) {
            this.signAlgorithm = signAlgorithm;
            return this;
        }

        @Nonnull
        public Builder setCertId(@Nonnull CertId certId) {
            this.certId = certId;
            return this;
        }

        @Nonnull
        public Builder setChaincodeId(@Nonnull ChaincodeId chaincodeId) {
            this.chaincodeId = chaincodeId;
            return this;
        }

        @Nonnull
        public Builder setCodeType(@Nonnull ChaincodeDeploy.CodeType codeType) {
            this.codeType = codeType;
            return this;
        }

        @Nonnull
        public Builder setSpcPackage(@Nonnull String spcPackage) {
            this.spcPackage = spcPackage;
            return this;
        }

        @Nonnull
        public Builder setLegal_prose(@Nonnull String legal_prose) {
            this.legal_prose = legal_prose;
            return this;
        }

        @Nonnull
        public Builder setTimeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        @Nonnull
        public Builder setPrivateKey(@Nonnull PrivateKey privateKey) {
            this.privateKey = privateKey;
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

    public void setSignAlgorithm(String signAlgorithm) {
        this.signAlgorithm = signAlgorithm;
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

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }
}
