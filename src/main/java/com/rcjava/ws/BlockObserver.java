package com.rcjava.ws;

import com.rcjava.protos.Peer;

/**
 * @author zyf
 */
public interface BlockObserver {

    /**
     * 回调Message-->Block
     *
     * @param block
     */
    void onMessage(Peer.Block block);
}
