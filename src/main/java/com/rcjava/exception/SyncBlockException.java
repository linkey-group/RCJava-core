package com.rcjava.exception;

public class SyncBlockException extends RuntimeException{

    public SyncBlockException() {
        super();
    }

    public SyncBlockException(String message) {
        super(message);
    }

    public SyncBlockException(String message, Throwable cause) {
        super(message, cause);
    }

    public SyncBlockException(Throwable cause) {
        super(cause);
    }

    public SyncBlockException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
