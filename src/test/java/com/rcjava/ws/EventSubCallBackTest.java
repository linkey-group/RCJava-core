package com.rcjava.ws;

import com.rcjava.protos.Peer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.sleep;

/**
 * @author zyf
 */
public class EventSubCallBackTest implements SubBasic {

    @Override
    public void onMessage(Peer.Block block) {
        System.err.println(block);
    }

    /**
     * 可选session
     * @param block
     * @param session
     */
    public void onMessage(Peer.Block block, Session session) {
        System.err.println(session.getId());
    }

    @Test
    void testOnMessageBySocket() throws IOException, DeploymentException, InterruptedException {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.connectToServer(new EventSocket(this), URI.create("ws://localhost:8081/event"));
        sleep(1000000);
    }

    @Test
    @DisplayName("测试EndPoint回调")
    void testOnMessageByEndPoint() throws IOException, DeploymentException, InterruptedException {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        List decoderList = new ArrayList<Class>();
        decoderList.add(EventDecoder.class);
        container.connectToServer(
                new EventEndPoint((block) -> onMessage(block)),
                ClientEndpointConfig.Builder.create().decoders(decoderList).build(),
                URI.create("ws://localhost:8081/event"));
        sleep(1000000);
    }
}
