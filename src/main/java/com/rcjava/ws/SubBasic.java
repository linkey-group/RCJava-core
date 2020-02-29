package com.rcjava.ws;

import com.rcjava.protos.Peer;

import javax.websocket.Session;

/**
 * @author zyf
 */
public interface SubBasic {

    /**
     * 回调Message
     *
     * @param block
     */
    default void onMessage(Peer.Block block, Session session) {
        onMessage(block);
    }

    /**
     * 回调Message
     *
     * @param block
     */
    void onMessage(Peer.Block block);

}
