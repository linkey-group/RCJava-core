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
    private ChaincodeId chaincodeId;
    private ChaincodeDeploy chaincodeDeploy;
    private int gasLimit;
    private String oid;
    private CertId certId;
    private PrivateKey privateKey;
    private String signAlgorithm;

    private DeployTran(Builder builder) {
        txid = builder.txid;
        chaincodeId = builder.chaincodeId;
        chaincodeDeploy = builder.chaincodeDeploy;
        gasLimit = builder.gasLimit;
        oid = builder.oid;
        certId = builder.certId;
        privateKey = builder.privateKey;
        signAlgorithm = builder.signAlgorithm;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(@Nonnull DeployTran copy) {
        Builder builder = new Builder();
        builder.txid = copy.getTxid();
        builder.chaincodeId = copy.getChaincodeId();
        builder.chaincodeDeploy = copy.getChaincodeDeploy();
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
                .setCertId(certId)
                .setChaincodeId(chaincodeId)
                .setChaincodeDeploy(chaincodeDeploy)
                .setGasLimit(gasLimit)
                .setOid(oid)
                .setPrivateKey(privateKey)
                .setSignAlgorithm(signAlgorithm);
    }


    /**
     * 使用privateKey和相应算法进行签名
     *
     * @param privateKey    私钥
     * @param signAlgorithm 签名算法
     * @return
     */
    @Override
    public Transaction getSignedTran(@Nonnull PrivateKey privateKey, @Nonnull String signAlgorithm) {
        String localTxid = UUID.randomUUID().toString().replace("-", "");
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
        private ChaincodeId chaincodeId;
        private ChaincodeDeploy chaincodeDeploy;
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
        public Builder setChaincodeDeploy(@Nonnull ChaincodeDeploy val) {
            chaincodeDeploy = val;
            return this;
        }

        @Nonnull
        public Builder setGasLimit(int val) {
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
        public DeployTran build() {
            return new DeployTran(this);
        }
    }

    public String getTxid() {
        return txid;
    }

    public ChaincodeId getChaincodeId() {
        return chaincodeId;
    }

    public ChaincodeDeploy getChaincodeDeploy() {
        return chaincodeDeploy;
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
