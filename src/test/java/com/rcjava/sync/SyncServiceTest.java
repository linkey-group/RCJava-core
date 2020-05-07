package com.rcjava.sync;

import com.rcjava.client.ChainInfoClient;
import com.rcjava.exception.SyncBlockException;
import com.rcjava.protos.Peer;
import com.rcjava.ws.BlockListener;
import com.rcjava.ws.BlockListenerUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static java.lang.Thread.sleep;

/**
 * @author zyf
 */
public class SyncServiceTest implements SyncListener, SyncEndPoint{

    Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    @DisplayName("测试同步区块服务")
    void testSyncBlock() throws InterruptedException {

        String host = "192.168.2.69:8081";

        long locHeight = 1L;
        String locBlkHash = new ChainInfoClient(host).getBlockByHeight(1).getHashOfBlock().toStringUtf8();

        SyncInfo syncInfo = new SyncInfo(locHeight, locBlkHash);

        SyncService syncService = SyncService.newBuilder()
                .setHost(host)
                .setSyncInfo(syncInfo)
                .setSyncListener(this)
                .setSyncEndPoint(this)
                .build();

        CountDownLatch latch = new CountDownLatch(1);

        Thread thread = new Thread(syncService::start);

        thread.start();

        latch.await();
        // 使用main的时候挂起就好了
//        thread.join();

        // 该测试方法里使用
//        sleep(60000);
//        syncService.stop();
//        sleep(7200000); // 使用latch代替
    }

    @Override
    public void onBlock(Peer.Block block) {
        try {
            sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.error(Thread.currentThread().getName() + " : " + String.format("当前块高度为：%s, 当前块Hash为：%s, 前块Hash为：%s",
                block.getHeight(),
                block.getHashOfBlock().toStringUtf8(),
                block.getPreviousBlockHash().toStringUtf8())
        );
    }

    @Override
    public void onError(SyncBlockException syncBlockException, long currentRemoteBlockHeight, String currentRemoteBlockPrevHash) {
        System.out.println(syncBlockException.getMessage());
    }

    @Override
    public String queryBlockHash(long blockHeight) {
        // TODO 业务端需提供根据高度获取本地已经保存的块Hash
        return null;
    }

    @Override
    public void update(long localLastCorrectHeight, long currentRemoteHeight, List<Peer.Block> correctBlockList) {
        // TODO 业务端使用正确的块列表，更新数据库
    }

    public static void main(String[] args) throws InterruptedException {
        new SyncServiceTest().testSyncBlock();
    }

    @Test
    @DisplayName("临时备份")
    void testTemp() {
//        blkObserver = blkObserver;
//        // 使用 host 获取一个监听，每个 host 对应一个监听
//        this.listener = BlockListener.getListener(host);
//        // event 监听，并回调给具体的实现类
//        listener.registerBlkObserver(blkObserver);
    }
}
