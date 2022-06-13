package com.rcjava.client;


import com.neovisionaries.ws.client.*;
import com.rcjava.ws.BlockListener;
import com.rcjava.ws.BlockListenerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * webSocket 订阅
 *
 * @author zyf
 */
public class RSubClient {

    private String host;
    private BlockListener blkListener;
    private SSLContext sslContext;
    private boolean useSsl = false;

    private WebSocket socket = null;
    private WebSocketFactory factory = new WebSocketFactory().setConnectionTimeout(10 * 1000);

    private static ConcurrentHashMap<String, RSubClient> staticSubClient = new ConcurrentHashMap<>();

    private Logger logger = LoggerFactory.getLogger(getClass());

    public RSubClient(String host, BlockListener blkListener) {
        this.host = host;
        this.blkListener = blkListener;
    }

    public RSubClient(String host, BlockListener blkListener, SSLContext sslContext) {
        this.host = host;
        this.blkListener = blkListener;
        this.sslContext = sslContext;
        this.factory = factory.setSSLContext(sslContext).setVerifyHostname(false);
        this.useSsl = true;
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
     * 获取实例，并放到Map里，保证一个host一个实例
     *
     * @param host
     * @return
     */
    public static RSubClient getRSubClient(String host, SSLContext sslContext) {
        String ssl_host = "ssl_" + host;
        if (!staticSubClient.containsKey(ssl_host)) {
            synchronized (RSubClient.class) {
                if (!staticSubClient.containsKey(ssl_host)) {
                    RSubClient rSubClient = new RSubClient(host, BlockListenerUtil.getListener(host), sslContext);
                    staticSubClient.put(ssl_host, rSubClient);
                }
            }
        }
        return staticSubClient.get(ssl_host);
    }

    /**
     * 链接socket，开启订阅
     *
     * @throws IOException
     */
    public void connect() throws IOException, WebSocketException {
        String protocol = useSsl ? "wss" : "ws";
        socket = factory.createSocket(String.format("%s://%s/event", protocol, host)).addListener(blkListener)
                .setPingSenderName("RSubClient")
                // set pingInterval to socket keepAlive
                .setPingInterval(30 * 1000).setPingPayloadGenerator(() -> {
                    // The string representation of the current date.
                    return new Date().toString().getBytes();
                }).connect();
    }

    /**
     * 断掉链接
     */
    public void disconnect() {
        socket.disconnect();
    }

    public boolean isopen() {
        return socket.isOpen();
    }

    /**
     * 判断是否已连接
     *
     * @return
     */
    public boolean isclosed() {
        return socket.getSocket().isClosed();
    }

    /**
     * 重连链接
     *
     * @throws IOException
     * @throws WebSocketException
     */
    public void reconnect() throws IOException, WebSocketException {
        socket = socket.recreate().connect();
    }

    public String getHost() {
        return host;
    }

    public BlockListener getBlkListener() {
        return blkListener;
    }

    public WebSocket getSocket() {
        return socket;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }
}
