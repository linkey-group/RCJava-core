package com.rcjava.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

/**
 * @author zyf
 */
public class ProviderUtil {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

}
