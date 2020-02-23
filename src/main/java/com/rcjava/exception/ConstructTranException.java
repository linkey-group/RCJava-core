package com.rcjava.exception;

/**
 * 构造交易异常
 *
 * @author zyf
 */
public class ConstructTranException extends RuntimeException {

    public ConstructTranException() {
    }

    public ConstructTranException(String message) {
        super(message);
    }

    public ConstructTranException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConstructTranException(Throwable cause) {
        super(cause);
    }

    public ConstructTranException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
