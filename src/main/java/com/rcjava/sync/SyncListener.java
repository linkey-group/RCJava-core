package com.rcjava.sync;

import com.rcjava.exception.SyncBlockException;
import com.rcjava.protos.Peer;

/**
 * @author zyf
 */
public interface SyncListener {

    /**
     * 正确同步时，推送块给同步者
     *
     * @param block
     */
    void onBlock(Peer.Block block);

    /**
     * 同步异常，如：RepChain某个块回滚了或者落盘失败，这个时候需要纠正数据库，重新开始，目前交给业务端来实现
     *
     * @param syncBlockException
     */
    void onError(SyncBlockException syncBlockException);
}
