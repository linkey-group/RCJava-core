package com.rcjava.gm;

import java.security.Provider;
import java.security.Security;

/**
 * 使用国密时，继承该类，或者在引入国密包之后，使用以下方式自行添加
 * <pre>
 *     Security.insertProviderAt(new BouncyCastleProvider(), 1);
 *     Security.insertProviderAt(new BouncyCastleJsseProvider(), 2);
 * </pre>
 */
public class GMProvider {

    static {
        try {
            Security.insertProviderAt((Provider) Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider").newInstance(), 1);
            Security.insertProviderAt((Provider) Class.forName("org.bouncycastle.jsse.provider.BouncyCastleJsseProvider").newInstance(), 2);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

}
