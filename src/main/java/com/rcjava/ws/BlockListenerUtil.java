package com.rcjava.ws;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 事件监听工厂
 *
 * @author zyf
 */
public class BlockListenerUtil {

    /**
     * 全局的
     */
    private final static ConcurrentHashMap<String, BlockListener> HOST_LISTENER = new ConcurrentHashMap<>();

    /**
     * 每个host只给一个listener
     *
     * @param host
     * @return
     */
    public static BlockListener getListener(String host) {
        if (!HOST_LISTENER.containsKey(host)) {
            BlockListener instance = new BlockListener();
            HOST_LISTENER.put(host, instance);
        }
        return HOST_LISTENER.get(host);
    }

}


