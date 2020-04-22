package com.rcjava.ws;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.rcjava.protos.Peer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * 事件监听
 *
 * @author zyf
 */
public class BlockListener extends WebSocketAdapter {

    /**
     * 实例自有的
     */
    private List<BlockObserver> blkObserverList = new Vector<>();

    private Logger logger = LoggerFactory.getLogger(getClass());

    protected BlockListener() { }


    /**
     * 注册observer
     *
     * @param blkObserver
     * @return
     */
    public boolean registerBlkObserver(@Nonnull BlockObserver blkObserver) {
        if (blkObserverList.contains(blkObserver)) {
            throw new RuntimeException("blkObserver 已存在");
        }
        return blkObserverList.add(blkObserver);
    }

    /**
     * 移除observer
     *
     * @param blkObserver
     * @return
     */
    public boolean removeBlkObserver(@Nonnull BlockObserver blkObserver) {
        return blkObserverList.remove(blkObserver);
    }

    /**
     * @param websocket
     * @param message
     * @throws Exception
     */
    @Override
    public void onBinaryMessage(WebSocket websocket, byte[] message) throws Exception {
        Peer.Event event = Peer.Event.parseFrom(message);
        int newBlock = 2;
        if (event.hasBlk() && event.getActionValue() == newBlock) {
            blkObserverList.forEach(blockObserver -> blockObserver.onMessage(event.getBlk()));
        }
    }

    @Override
    public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
        logger.info("webSocket connected {}", websocket);
    }

    @Override
    public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
        logger.error("webSocket disconnect, isClosedByServer: {}", closedByServer);
    }

    @Override
    public void onCloseFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
        logger.error("webSocket closed, the code is {}, the reason is {}", frame.getCloseCode(), frame.getCloseReason());
    }

    @Override
    public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
        logger.error("webSocket error {}", cause.getMessage(), cause);
    }

    @Override
    public void onUnexpectedError(WebSocket websocket, WebSocketException cause) throws Exception {
        logger.error("webSocket unExpectedError: {}", cause.getMessage(), cause.getCause());
        throw cause;
    }

}