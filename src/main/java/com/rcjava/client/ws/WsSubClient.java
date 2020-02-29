package com.rcjava.client.ws;

import com.rcjava.ws.EventSocket;
import com.rcjava.ws.SubBasic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutorService;

/**
 * 订阅RepChain webSocket数据
 *
 * @author zyf
 */
public class WsSubClient {

    private String host;
    private SubBasic subBasic;
    private EventSocket eventSocket;
    private WebSocketContainer container = ContainerProvider.getWebSocketContainer();

    private Logger logger = LoggerFactory.getLogger(getClass());


    public WsSubClient(@Nonnull String host, @Nonnull SubBasic subBasic) {
        this.host = host;
        this.subBasic = subBasic;
        this.eventSocket = new EventSocket(subBasic);
    }

    /**
     *
     * @throws IOException
     * @throws DeploymentException
     */
    public void start() throws IOException, DeploymentException {
        container.connectToServer(eventSocket, URI.create(String.format("ws://%s/event", host)));
    }

    /**
     *
     * @throws IOException
     */
    public void close() throws IOException {
        Session session = eventSocket.getSession();
        if (session.isOpen()) {
            session.close();
            logger.info("close the socket, sessionId : {}", session.getId());
        }
        ExecutorService service = eventSocket.getExecutorService();
        if (service != null) {
            service.shutdown();
            logger.info("shutdown the ExecutorService : {}", service);
        }
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public SubBasic getSubBasic() {
        return subBasic;
    }

    public void setSubBasic(SubBasic subBasic) {
        this.subBasic = subBasic;
    }

}
