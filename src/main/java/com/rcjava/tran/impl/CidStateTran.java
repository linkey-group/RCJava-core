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
    private ChaincodeId chaincodeId;
    private Boolean state;
    private int gasLimit;
    private String oid;
    private CertId certId;
    private PrivateKey privateKey;
    private String signAlgorithm;

    private CidStateTran(Builder builder) {
        txid = builder.txid;
        chaincodeId = builder.chaincodeId;
        state = builder.state;
        gasLimit = builder.gasLimit;
        oid = builder.oid;
        certId = builder.certId;
        privateKey = builder.privateKey;
        signAlgorithm = builder.signAlgorithm;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(@Nonnull CidStateTran copy) {
        Builder builder = new Builder();
        builder.txid = copy.getTxid();
        builder.chaincodeId = copy.getChaincodeId();
        builder.state = copy.getState();
        builder.gasLimit = copy.getGasLimit();
        builder.oid = copy.getOid();
        builder.certId = copy.getCertId();
        builder.privateKey = copy.getPrivateKey();
        builder.signAlgorithm = copy.getSignAlgorithm();
        return builder;
    }

    public Builder toBuilder() {
        return new Builder()
                .setTxid(txid)
                .setChaincodeId(chaincodeId)
                .setState(state)
                .setGasLimit(gasLimit)
                .setOid(oid)
                .setCertId(certId)
                .setPrivateKey(privateKey)
                .setSignAlgorithm(signAlgorithm);
    }


    /**
     *
     * @param privateKey    私钥
     * @param signAlgorithm 签名算法
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
        private ChaincodeId chaincodeId;
        private Boolean state;
        private int gasLimit = 0;
        private String oid = "";
        private CertId certId;
        private PrivateKey privateKey;
        private String signAlgorithm;

        private Builder() {
        }

        @Nonnull
        public Builder setTxid(@Nonnull String val) {
            txid = val;
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
        public Builder setCertId(@Nonnull CertId val) {
            certId = val;
            return this;
        }

        @Nonnull
        public Builder setPrivateKey(@Nonnull PrivateKey val) {
            privateKey = val;
            return this;
        }

        @Nonnull
        public Builder setSignAlgorithm(@Nonnull String val) {
            signAlgorithm = val;
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

    public ChaincodeId getChaincodeId() {
        return chaincodeId;
    }

    public Boolean getState() {
        return state;
    }

    public int getGasLimit() {
        return gasLimit;
    }

    public String getOid() {
        return oid;
    }

    public CertId getCertId() {
        return certId;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public String getSignAlgorithm() {
        return signAlgorithm;
    }
}
