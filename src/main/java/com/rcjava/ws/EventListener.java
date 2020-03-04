package com.rcjava.ws;

import com.neovisionaries.ws.client.*;
import com.rcjava.protos.Peer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

/**
 * @author zyf
 */
public class EventListener extends WebSocketAdapter{

    private EventSubBase eventSubBase;

    private Logger logger = LoggerFactory.getLogger(getClass());

    public EventListener(@Nonnull EventSubBase eventSubBase) {
        this.eventSubBase = eventSubBase;
    }


    @Override
    public void onBinaryMessage(WebSocket websocket, byte[] message) throws Exception {
        Peer.Event event = Peer.Event.parseFrom(message);
        if (event.hasBlk()) {
            eventSubBase.onMessage(event.getBlk());
        }
    }

    @Override
    public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
        logger.error("webSocket error {}", cause.getMessage(), cause);
        websocket.recreate(30000).connect();
        logger.info("webSocket reconnect");
    }

    @Override
    public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
        logger.info("webSocket connected {}", websocket);
    }

    @Override
    public void onCloseFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
        logger.error("webSocket closed, the code is {}, the reason is {}", frame.getCloseCode(), frame.getCloseReason());
        websocket.recreate(30000).connect();
        logger.info("webSocket reconnect");
    }

    @Override
    public void onUnexpectedError(WebSocket websocket, WebSocketException cause) throws Exception {
        logger.error("webSocket unExpectedError: {}", cause.getMessage(), cause.getCause());
    }

}
