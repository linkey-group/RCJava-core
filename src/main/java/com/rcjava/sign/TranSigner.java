package com.rcjava.sign;

import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import com.rcjava.exception.ConstructTranException;
import com.rcjava.protos.Peer.*;
import com.rcjava.sign.impl.ECDSASign;

import java.security.PrivateKey;

/**
 * @author zyf
 */
public class TranSigner {

    /**
     * 签名交易
     *
     * @param tranWithOutSign
     * @param certId
     * @param privateKey
     * @param signAlgorithm
     * @return transactionWithSign
     */
    public static Transaction signTran(Transaction tranWithOutSign, CertId certId, PrivateKey privateKey, String signAlgorithm) {
        if (!isInitial(privateKey, signAlgorithm)) {
            throw new ConstructTranException("私钥或者算法为空");
        }
        byte[] sig = new ECDSASign(signAlgorithm).sign(privateKey, tranWithOutSign.toByteArray());
        long millis = System.currentTimeMillis() + 8 * 3600 * 1000;
        Signature signature = Signature.newBuilder()
                .setCertId(certId)
                .setTmLocal(Timestamp.newBuilder()
                        .setSeconds(millis / 1000)
                        .setNanos((int) (millis % 1000) * 1000000))
                .setSignature(ByteString.copyFrom(sig))
                .build();
        Transaction.Builder builder = tranWithOutSign.toBuilder();
        builder.setSignature(signature);
        return builder.build();
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

    // TODO
    public class Generator {

        private PrivateKey privateKey;
        private String signAlgorithm;

    }
}
