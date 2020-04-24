package com.rcjava.ws;

import com.rcjava.protos.Peer;

/**
 * @author zyf
 */
public interface BlockObserver {

    /**
     * 回调Message-->Block
     *
     * @param block 回调的块数据
     */
    void onMessage(Peer.Block block);
}
