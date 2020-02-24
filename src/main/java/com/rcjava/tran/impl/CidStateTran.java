package com.rcjava.tran.impl;

import com.rcjava.protos.Peer.*;
import com.rcjava.tran.RCTran;
import com.rcjava.sign.TranSigner;

import javax.annotation.Nonnull;
import java.security.PrivateKey;
import java.util.UUID;

/**
 * @author zyf
 */
public class CidStateTran implements RCTran {

    private String txid;
    private String signAlgorithm;
    private CertId certId;
    private ChaincodeId chaincodeId;
    private Boolean state;
    private PrivateKey privateKey;

    private CidStateTran(Builder builder) {
        txid = builder.txid;
        signAlgorithm = builder.signAlgorithm;
        certId = builder.certId;
        chaincodeId = builder.chaincodeId;
        state = builder.state;
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
                .setState(state)
                .setPrivateKey(privateKey);
    }


    /**
     *
     * @param privateKey
     * @param signAlgorithm
     * @return
     */
    @Override
    public Transaction getSignedTran(PrivateKey privateKey, String signAlgorithm) {
        String localTxid = UUID.randomUUID().toString().replace("-", "");
        Transaction tranSt = Transaction.newBuilder()
                .setId(txid == null ? localTxid : txid)
                .setType(Transaction.Type.CHAINCODE_SET_STATE)
                .setCid(chaincodeId)
                .setState(state)
                .build();
        return TranSigner.signTran(tranSt, certId, privateKey, signAlgorithm);
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
        private Boolean state = true;
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
        public Builder setState(@Nonnull Boolean state) {
            this.state = state;
            return this;
        }

        @Nonnull
        public Builder setPrivateKey(@Nonnull PrivateKey privateKey) {
            this.privateKey = privateKey;
            return this;
        }

        @Nonnull
        public CidStateTran build() {
            return new CidStateTran(this);
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

    public Boolean getState() {
        return state;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }
}
