package com.rcjava.ws;


import com.rcjava.protos.Peer;

/**
 * @author zyf
 */
public class BlockSub implements EventSubBase {


    /**
     * 回调Message
     *
     * @param block
     */
    @Override
    public void onMessage(Peer.Block block) {

    }
}
