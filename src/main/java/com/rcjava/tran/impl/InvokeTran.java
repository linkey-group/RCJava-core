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
    private ChaincodeId chaincodeId;
    private ChaincodeInput chaincodeInput;
    private int gasLimit;
    private String oid;
    private CertId certId;
    private PrivateKey privateKey;
    private String signAlgorithm;

    private InvokeTran(Builder builder) {
        txid = builder.txid;
        chaincodeId = builder.chaincodeId;
        chaincodeInput = builder.chaincodeInput;
        gasLimit = builder.gasLimit;
        oid = builder.oid;
        certId = builder.certId;
        privateKey = builder.privateKey;
        signAlgorithm = builder.signAlgorithm;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(@Nonnull InvokeTran copy) {
        Builder builder = new Builder();
        builder.txid = copy.getTxid();
        builder.chaincodeId = copy.getChaincodeId();
        builder.chaincodeInput = copy.getChaincodeInput();
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
                .setChaincodeInput(chaincodeInput)
                .setGasLimit(gasLimit)
                .setOid(oid)
                .setCertId(certId)
                .setPrivateKey(privateKey)
                .setSignAlgorithm(signAlgorithm);
    }

    /**
     * @param privateKey    私钥
     * @param signAlgorithm 签名算法
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
        private ChaincodeId chaincodeId;
        private ChaincodeInput chaincodeInput;
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
        public InvokeTran build() {
            return new InvokeTran(this);
        }
    }

    public String getTxid() {
        return txid;
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
