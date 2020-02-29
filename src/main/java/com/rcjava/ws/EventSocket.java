package com.rcjava.ws;


import com.rcjava.protos.Peer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.*;

/**
 * 批注驱动，与websocket生命周期进行交互；
 * 也可以使用接口驱动，如实现EndPoint接口{@link EventEndPoint}
 *
 * @author zyf
 */
@ClientEndpoint(decoders = EventDecoder.class)
public class EventSocket {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private ScheduledExecutorService executorService = null;
    private SubBasic subBasic;
    private Session session;

    public EventSocket() {
    }

    public EventSocket(@Nonnull SubBasic subBasic) {
        this.subBasic = subBasic;
    }

    @OnOpen
    public void onWebSocketConnect(Session session) {
        if (executorService != null && !executorService.isShutdown()) {
            logger.info("shutdown executorService {}", executorService);
            executorService.shutdown();
            executorService = null;
        }
        this.session = session;
        logger.info("Socket Connected: {}", session);
    }

    @OnMessage
    public void onWebSocketMessage(Peer.Event event, Session session) {
        try {
            if (subBasic != null) {
                if (event.hasBlk()) {
                    Peer.Block block = event.getBlk();
                    subBasic.onMessage(block, session);
                }
            } else {
                logger.info("缺SubBasic实现，无法回调onMessage");
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    @OnClose
    public void onWebSocketClose(CloseReason reason, Session session) {
        reconnect(session);
        logger.info("Socket Closed: {}, start reconnect", reason);
    }

    @OnError
    public void onWebSocketError(Throwable cause, Session session) {
        reconnect(session);
        logger.error("Socket Error message: {}, start reconnect", cause.getMessage(), cause);
//        cause.printStackTrace(System.err);
    }

    /**
     * 重连websocket
     *
     * @param session
     */
    private void reconnect(Session session) {
        if (executorService == null) {
            executorService = Executors.newSingleThreadScheduledExecutor();
            logger.info("create new executorService: {}", executorService);
        }
        executorService.scheduleAtFixedRate(() -> {
            if (!session.isOpen()) {
                try {
                    session.getContainer().connectToServer(this, session.getRequestURI());
                } catch (DeploymentException | IOException e) {
                    logger.error(e.getMessage(), e);
                    e.printStackTrace();
                }
            }
        }, 5, 30, TimeUnit.SECONDS);

    }

    public void setSubBasic(SubBasic subBasic) {
        this.subBasic = subBasic;
    }

    public ScheduledExecutorService getExecutorService() {
        return executorService;
    }

    public Session getSession() {
        return session;
    }
}
