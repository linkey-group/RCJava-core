package com.rcjava.ws;

import com.rcjava.protos.Peer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.websocket.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 接口驱动，也可使用批注驱动{@link EventSocket}
 *
 * @author zyf
 */
public class EventEndPoint extends Endpoint {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private ScheduledExecutorService executorService = null;
    private ClientEndpointConfig endpointConfig = null;
    private SubBasic subBasic;
    private Session session;

    public EventEndPoint() {
    }

    public EventEndPoint(@Nonnull SubBasic subBasic) {
        this.subBasic = subBasic;
    }

    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        if (executorService != null && !executorService.isShutdown()) {
            logger.info("shutdown executorService");
            executorService.shutdown();
            executorService = null;
        }
        session.addMessageHandler((MessageHandler.Whole<Peer.Event>) (message) -> {
            if (subBasic != null) {
                if (message.hasBlk()) {
                    Peer.Block block = message.getBlk();
                    subBasic.onMessage(block, session);
                }
            } else {
                logger.info("缺SubBasic实现，无法回调onMessage");
            }

        });
        this.session = session;
        this.endpointConfig = (ClientEndpointConfig) endpointConfig;
        logger.info("Socket Connected: {}", session);
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        reconnect(session);
        logger.info("Socket Closed: {}, start reconnect", closeReason);
    }

    @Override
    public void onError(Session session, Throwable throwable) {
        reconnect(session);
        logger.error("Socket Error message: {}, start reconnect", throwable.getMessage(), throwable);
    }

    private void reconnect(Session session) {
        if (executorService == null) {
            executorService = Executors.newSingleThreadScheduledExecutor();
            logger.info("create new executorService: {}", executorService);
        }
        executorService.scheduleAtFixedRate(() -> {
            if (!session.isOpen()) {
                try {
                    session.getContainer().connectToServer(this, endpointConfig, session.getRequestURI());
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
