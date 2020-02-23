package com.rcjava.sign;

import com.rcjava.exception.ConstructTranException;
import com.rcjava.protos.Peer.Transaction;
import com.rcjava.tran.RCTran;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PrivateKey;

/**
 * 获取签名交易
 *
 * @author zyf
 */
public class RCTranSigner {

    private Logger logger = LoggerFactory.getLogger(getClass());
    private static Logger staticLogger = LoggerFactory.getLogger(RCTranSigner.class);


    /**
     * @param rcTran        部署交易、调用交易、修改合约状态的交易
     * @param privateKey    私钥
     * @param signAlgorithm 签名算法
     * @return
     */
    public static Transaction getSignedTran(RCTran rcTran, PrivateKey privateKey, String signAlgorithm) {
        if (!isInitial(privateKey, signAlgorithm)) {
            throw new ConstructTranException("私钥或者算法为空");
        }
        return rcTran.getSignedTran(privateKey, signAlgorithm);
    }

    /**
     *
     * @param privateKey
     * @param signAlgorithm
     * @return
     */
    private static boolean isInitial (PrivateKey privateKey, String signAlgorithm) {
        return privateKey != null && signAlgorithm != null;
    }

}
