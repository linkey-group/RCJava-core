package com.rcjava.ws;

import com.rcjava.client.ws.WsSubClient;
import com.rcjava.protos.Peer;
import org.junit.jupiter.api.Test;

import javax.websocket.DeploymentException;
import javax.websocket.Session;
import java.io.IOException;

/**
 * @author zyf
 */
public class WsSubClientTest implements SubBasic {

    @Test
    void testStart() throws IOException, DeploymentException, InterruptedException {

        String host = "localhost:8081";
        WsSubClient wsSubClient = new WsSubClient(host, this);
        wsSubClient.start();
        Thread.sleep(100000);
        wsSubClient.close();
        Thread.sleep(160000);
    }

    @Override
    public void onMessage(Peer.Block block) {
        System.out.println(block);
    }

    /**
     * 可选session，这个方法如果删除就用的是上面的方法
     *
     * @param block
     * @param session
     */
    public void onMessage(Peer.Block block, Session session) {
        System.err.println(session.getId());
    }
}
