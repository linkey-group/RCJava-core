package com.rcjava.client;


import com.neovisionaries.ws.client.*;
import com.rcjava.ws.BlockListener;
import com.rcjava.ws.BlockListenerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * webSocket 订阅
 *
 * @author zyf
 */
public class RSubClient {

    private String host = "localhost:8081";
    private BlockListener blkListener;
    private WebSocket ws = null;

    private static WebSocketFactory factory = new WebSocketFactory();

    private static ConcurrentHashMap<String, RSubClient> staticSubClient = new ConcurrentHashMap<>();

    private Logger logger = LoggerFactory.getLogger(getClass());

    public RSubClient(String host, BlockListener blkListener) {
        this.host = host;
        this.blkListener = blkListener;
    }

    /**
     * 获取实例，并放到Map里，保证一个host一个实例
     *
     * @param host
     * @return
     */
    public static RSubClient getRSubClient(String host) {
        if (!staticSubClient.containsKey(host)) {
            synchronized (RSubClient.class) {
                if (!staticSubClient.containsKey(host)) {
                    RSubClient rSubClient = new RSubClient(host, BlockListenerUtil.getListener(host));
                    staticSubClient.put(host, rSubClient);
                }
            }
        }
        return staticSubClient.get(host);
    }

    /**
     * 链接socket，开启订阅
     *
     * @throws IOException
     */
    public void connect() throws IOException {
        ws = factory
                .setConnectionTimeout(10 * 1000)
                .createSocket(String.format("ws://%s/event", host))
                .addListener(blkListener);

        try {
            ws.connect();
        } catch (OpeningHandshakeException e) {
            // Status line.
            StatusLine sl = e.getStatusLine();
            String httpVersion = sl.getHttpVersion();
            int statusCode = sl.getStatusCode();
            String reason = sl.getReasonPhrase();
            logger.error(String.format("httpVesion: %s, status code: %s, reason: %s\n", httpVersion, statusCode, reason), e);
        } catch (WebSocketException e) {
            // Failed to establish a WebSocket connection.
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 断掉链接
     */
    public void disconnect() {
        ws.disconnect();
    }

    public boolean isopen() {
        return ws.isOpen();
    }

    /**
     * 判断是否已连接
     *
     * @return
     */
    public boolean isclosed() {
        return ws.getSocket().isClosed();
    }

    /**
     * 重连链接
     *
     * @throws IOException
     * @throws WebSocketException
     */
    public void reconnect() throws IOException, WebSocketException {
        ws = ws.recreate().connect();
    }

    public String getHost() {
        return host;
    }

    public BlockListener getBlkListener() {
        return blkListener;
    }

    public WebSocket getWs() {
        return ws;
    }
}
