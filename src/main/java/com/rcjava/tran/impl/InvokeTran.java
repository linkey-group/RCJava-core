package com.rcjava.tran.impl;

import com.rcjava.protos.Peer.*;
import com.rcjava.tran.RCTran;
import com.rcjava.sign.TranSigner;

import javax.annotation.Nonnull;
import java.security.PrivateKey;

/**
 * @author zyf
 */
public class InvokeTran implements RCTran {

    private String txid;
    private String signAlgorithm;
    private CertId certId;
    private ChaincodeId chaincodeId;
    private ChaincodeInput chaincodeInput;
    private Transaction invokeTran;
    private PrivateKey privateKey;

    private InvokeTran(Builder builder) {
        txid = builder.txid;
        signAlgorithm = builder.signAlgorithm;
        certId = builder.certId;
        chaincodeId = builder.chaincodeId;
        chaincodeInput = builder.chaincodeInput;
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
                .setChaincodeInput(chaincodeInput)
                .setPrivateKey(privateKey);
    }

    /**
     * @param privateKey
     * @param signAlgorithm
     * @return
     */
    @Override
    public Transaction getSignedTran(@Nonnull PrivateKey privateKey, @Nonnull String signAlgorithm) {
        Transaction tranInv = Transaction.newBuilder()
                .setId(txid)
                .setType(Transaction.Type.CHAINCODE_INVOKE)
                .setCid(chaincodeId)
                .setIpt(chaincodeInput)
                .build();
        this.invokeTran = TranSigner.signTran(tranInv, certId, privateKey, signAlgorithm);
        return invokeTran;
    }

    @Override
    public Transaction getSignedTran() {
        return this.getSignedTran(this.privateKey, this.signAlgorithm);
    }

    public static final class Builder {
        private String txid;
        private String signAlgorithm;
        private CertId certId;
        private ChaincodeId chaincodeId;
        private ChaincodeInput chaincodeInput;
        private PrivateKey privateKey;

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
        public Builder setChaincodeInput(@Nonnull ChaincodeInput chaincodeInput) {
            this.chaincodeInput = chaincodeInput;
            return this;
        }

        @Nonnull
        public Builder setPrivateKey(@Nonnull PrivateKey privateKey) {
            this.privateKey = privateKey;
            return this;
        }

        @Nonnull
        public InvokeTran build() {
            return new InvokeTran(this);
        }
    }

    public void setTxid(String txid) {
        this.txid = txid;
    }

    public void setChaincodeInput(ChaincodeInput chaincodeInput) {
        this.chaincodeInput = chaincodeInput;
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

    public ChaincodeInput getChaincodeInput() {
        return chaincodeInput;
    }

    public Transaction getInvokeTran() {
        return invokeTran;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }
}
