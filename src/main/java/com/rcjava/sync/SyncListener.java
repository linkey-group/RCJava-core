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
     * @param block 区块
     */
    void onBlock(Peer.Block block);

    /**
     * 同步异常，如：RepChain某个块回滚了或者落盘失败，这个时候需要纠正数据库，重新开始，目前交给业务端来实现（业务端需实现SyncEndPoint接口）
     *
     * @param syncBlockException         同步异常
     * @param currentRemoteBlockHeight   当前远端块高度
     * @param currentRemoteBlockPrevHash 当前远端块previousHash
     */
    void onError(SyncBlockException syncBlockException, long currentRemoteBlockHeight, String currentRemoteBlockPrevHash);
}
