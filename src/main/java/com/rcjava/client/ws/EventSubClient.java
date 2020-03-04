package com.rcjava.client.ws;

import com.neovisionaries.ws.client.*;
import com.rcjava.ws.EventSubBase;
import com.rcjava.ws.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author zyf
 */
public class EventSubClient {

    private String host = "localhost:8081";
    private EventSubBase eventSubBase;
    private EventListener listener;
    private WebSocket ws;

    private static WebSocketFactory factory = new WebSocketFactory();

    private Logger logger = LoggerFactory.getLogger(getClass());


    public EventSubClient(String host, EventSubBase eventSubBase) {
        this.host = host;
        this.eventSubBase = eventSubBase;
        // event 监听，并回调给具体的实现类
        this.listener = new EventListener(eventSubBase);
    }

    /**
     * 链接socket，开启订阅
     *
     * @throws IOException
     */
    void connect() throws IOException {
        ws = factory
                .setConnectionTimeout(30000)
                .createSocket(String.format("ws://%s/event", host))
                .addListener(listener);

        try {
            ws.connect();
        } catch (OpeningHandshakeException e) {
            // Status line.
            StatusLine sl = e.getStatusLine();
            String httpVersion = String.format("HTTP Version  = %s\n", sl.getHttpVersion());
            String statusCode = String.format("Status Code   = %d\n", sl.getStatusCode());
            String reason = String.format("Reason Phrase = %s\n", sl.getReasonPhrase());
            logger.error(String.format("httpVesion: %s, status code: %s, reason: %s", httpVersion, statusCode, reason), e);
        } catch (HostnameUnverifiedException e) {
            // The certificate of the peer does not match the expected hostname.
            logger.error(e.getMessage(), e);
        } catch (WebSocketException e) {
            // Failed to establish a WebSocket connection.
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 重连链接
     *
     * @throws IOException
     * @throws WebSocketException
     */
    void reconnect() throws IOException, WebSocketException {
        ws.recreate().connect();
    }

    /**
     * 断掉链接
     */
    void disconnect() {
        ws.disconnect();
    }

    public String getHost() {
        return host;
    }

    public WebSocket getWs() {
        return ws;
    }
}
