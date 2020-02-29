package com.rcjava.ws;

import com.rcjava.protos.Peer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.sleep;

/**
 * 使用例子，本项目不采用这种方式了，引入了回调{@link EventSubCallBackTest}
 * @author zyf
 */
@DisplayName("使用例子，本项目不采用这种方式了")
public class EventSubTest {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    @DisplayName("注解方式")
    void testOnMessageBySocket() throws IOException, DeploymentException, InterruptedException {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.connectToServer(EventSocket.class, URI.create("ws://localhost:8081/event"));
        sleep(1000000);
    }

    @Test
    @DisplayName("接口方式")
    void testOnMessageByEndPoint() throws IOException, DeploymentException, InterruptedException {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        List decoderList = new ArrayList<Class>();
        decoderList.add(EventDecoder.class);
        container.connectToServer(
                EventEndPoint.class,
                ClientEndpointConfig.Builder.create().decoders(decoderList).build(),
                URI.create("ws://localhost:8081/event"));
        sleep(1000000);
    }
}
