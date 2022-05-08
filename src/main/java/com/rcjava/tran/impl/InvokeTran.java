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
public class InvokeTran implements RCTran {

    private String txid;
    private String signAlgorithm;
    private CertId certId;
    private ChaincodeId chaincodeId;
    private ChaincodeInput chaincodeInput;
    private int gasLimit;
    private String oid;
    private PrivateKey privateKey;

    private InvokeTran(Builder builder) {
        txid = builder.txid;
        signAlgorithm = builder.signAlgorithm;
        certId = builder.certId;
        chaincodeId = builder.chaincodeId;
        chaincodeInput = builder.chaincodeInput;
        gasLimit = builder.gasLimit;
        oid = builder.oid;
        privateKey = builder.privateKey;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(@Nonnull InvokeTran copy) {
        Builder builder = new Builder();
        builder.txid = copy.getTxid();
        builder.signAlgorithm = copy.getSignAlgorithm();
        builder.certId = copy.getCertId();
        builder.chaincodeId = copy.getChaincodeId();
        builder.chaincodeInput = copy.getChaincodeInput();
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
                .setChaincodeInput(chaincodeInput)
                .setGasLimit(gasLimit)
                .setOid(oid)
                .setPrivateKey(privateKey);
    }

    /**
     * @param privateKey
     * @param signAlgorithm
     * @return
     */
    @Override
    public Transaction getSignedTran(@Nonnull PrivateKey privateKey, @Nonnull String signAlgorithm) {
        String localTxid = UUID.randomUUID().toString().replace("-", "");
        Transaction tranInv = Transaction.newBuilder()
                .setId(txid == null? localTxid : txid)
                .setType(Transaction.Type.CHAINCODE_INVOKE)
                .setCid(chaincodeId)
                .setIpt(chaincodeInput)
                .setGasLimit(gasLimit)
                .setOid(oid)
                .build();
        return TranSigner.signTran(tranInv, certId, privateKey, signAlgorithm);
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
        private ChaincodeInput chaincodeInput;
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
        public Builder setChaincodeInput(@Nonnull ChaincodeInput val) {
            chaincodeInput = val;
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
        public InvokeTran build() {
            return new InvokeTran(this);
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

    public ChaincodeInput getChaincodeInput() {
        return chaincodeInput;
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
