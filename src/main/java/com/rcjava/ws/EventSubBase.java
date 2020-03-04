package com.rcjava.ws;

/**
 * @author zyf
 */

import com.rcjava.protos.Peer;

public interface EventSubBase {

    /**
     * 回调Message
     *
     * @param block
     */
    void onMessage(Peer.Block block);
}
