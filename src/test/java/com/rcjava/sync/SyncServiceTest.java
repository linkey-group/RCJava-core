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

import java.util.concurrent.CountDownLatch;

import static java.lang.Thread.sleep;

/**
 * @author zyf
 */
public class SyncServiceTest implements SyncListener {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    @DisplayName("测试同步区块服务")
    void testSyncBlock() throws InterruptedException {

        String host = "192.168.2.69:8081";

        long locHeight = 0L;
        // 本地高度为0时，设置locBlkHash为""或者null
        SyncInfo syncInfo = new SyncInfo(locHeight, "");

//        long locHeight = 1L;
//        String locBlkHash = new ChainInfoClient(host).getBlockByHeight(locHeight).getHashOfBlock().toStringUtf8();

        SyncService syncService = SyncService.newBuilder()
                .setHost(host)
                .setSyncInfo(syncInfo)
                .setSyncListener(this)
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
    public void onSuccess(Peer.Block block) throws SyncBlockException {
        try {
            sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (block.getHeight() == 100) {
            throw new SyncBlockException("模拟数据库写入异常");
        }
        logger.error(Thread.currentThread().getName() + " : " + String.format("当前块高度为：%s, 当前块Hash为：%s, 前块Hash为：%s",
                block.getHeight(),
                block.getHashOfBlock().toStringUtf8(),
                block.getPreviousBlockHash().toStringUtf8())
        );
    }

    @Override
    public void onError(SyncBlockException syncBlockException) {
        System.out.println(syncBlockException.getMessage());
    }

    public static void main(String[] args) throws InterruptedException {
        new SyncServiceTest().testSyncBlock();
    }

}
