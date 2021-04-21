package com.rcjava.sync;

import com.rcjava.exception.SyncBlockException;
import com.rcjava.protos.Peer;

/**
 * @author zyf
 */
public interface SyncListener {

    /**
     * 正确同步时，推送区块给订阅者
     *
     * @param block 区块
     * @throws SyncBlockException 如果业务端未成功将区块数据写入数据库，则须抛出异常
     */
    void onSuccess(Peer.Block block) throws SyncBlockException;

    /**
     * 同步异常
     *
     * @param syncBlockException 同步异常
     */
    void onError(SyncBlockException syncBlockException);
}
