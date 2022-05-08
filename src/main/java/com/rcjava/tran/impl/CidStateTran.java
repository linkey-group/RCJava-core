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
    private int gasLimit;
    private String oid;

    private CidStateTran(Builder builder) {
        txid = builder.txid;
        signAlgorithm = builder.signAlgorithm;
        certId = builder.certId;
        chaincodeId = builder.chaincodeId;
        state = builder.state;
        privateKey = builder.privateKey;
        gasLimit = builder.gasLimit;
        oid = builder.oid;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(@Nonnull CidStateTran copy) {
        Builder builder = new Builder();
        builder.txid = copy.getTxid();
        builder.signAlgorithm = copy.getSignAlgorithm();
        builder.certId = copy.getCertId();
        builder.chaincodeId = copy.getChaincodeId();
        builder.state = copy.getState();
        builder.privateKey = copy.getPrivateKey();
        builder.gasLimit = copy.getGasLimit();
        builder.oid = copy.getOid();
        return builder;
    }

    public Builder toBuilder() {
        return new Builder()
                .setTxid(txid)
                .setSignAlgorithm(signAlgorithm)
                .setCertId(certId)
                .setChaincodeId(chaincodeId)
                .setState(state)
                .setPrivateKey(privateKey)
                .setGasLimit(gasLimit)
                .setOid(oid);
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
                .setGasLimit(gasLimit)
                .setOid(oid)
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
        private String txid;
        private String signAlgorithm;
        private CertId certId;
        private ChaincodeId chaincodeId;
        private Boolean state;
        private PrivateKey privateKey;
        private int gasLimit = 0;
        private String oid = "";

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
        public Builder setState(@Nonnull Boolean val) {
            state = val;
            return this;
        }

        @Nonnull
        public Builder setPrivateKey(@Nonnull PrivateKey val) {
            privateKey = val;
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

    public int getGasLimit() {
        return gasLimit;
    }

    public String getOid() {
        return oid;
    }
}
