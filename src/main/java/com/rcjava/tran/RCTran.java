package com.rcjava.tran;

import com.rcjava.protos.Peer.Transaction;

import java.security.PrivateKey;

/**
 * @author zyf
 */
public interface RCTran {

    /**
     * 使用privateKey 及 signAlgorithm进行签名
     *
     * @param privateKey
     * @param signAlgorithm
     * @return
     */
    Transaction getSignedTran(PrivateKey privateKey, String signAlgorithm);

    /**
     * 获取签名交易
     *
     * @return
     */
    Transaction getSignedTran();
}
