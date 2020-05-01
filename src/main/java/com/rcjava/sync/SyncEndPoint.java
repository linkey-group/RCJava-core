package com.rcjava.sync;

import com.rcjava.protos.Peer;

import java.util.List;

/**
 * <p>
 * 与业务端DataSource交互，业务端需实现该接口<br>
 * 接口主要是为了防止RepChain块数据落盘失败，或者块回滚
 * </p>
 *
 * @author zyf
 */
public interface SyncEndPoint {

    /**
     * 使用块高从同步数据库或者其他APP提供的途径获取块Hash
     *
     * @param blockHeight 块高度
     * @return blockHash
     */
    String queryBlockHash(long blockHeight);

    /**
     * 使用正确块列表更新数据库或者数据源
     *
     * @param localLastCorrectHeight 本地最后的正确块高度
     * @param currentRemoteHeight    当前远端块高
     * @param correctBlockList       二者之间的正确块列表
     */
    void update(long localLastCorrectHeight, long currentRemoteHeight, List<Peer.Block> correctBlockList);
}
