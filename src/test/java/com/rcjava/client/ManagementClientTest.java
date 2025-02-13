package com.rcjava.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContexts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static com.google.common.truth.Truth.assertThat;


/**
 * @author zyf
 */
public class ManagementClientTest {

    SSLContext sslContext = SSLContexts.custom()
            .loadTrustMaterial(new File("jks/jdk13/121000005l35120456.node1.jks"), "123".toCharArray(), new TrustSelfSignedStrategy())
            .loadKeyMaterial(new File("jks/jdk13/121000005l35120456.node1.jks"), "123".toCharArray(), "123".toCharArray())
            .build();

    //    private ChainInfoClient chainInfoClient = new ChainInfoClient("localhost:9081", sslContext);
    private ManagementClient managementClient = new ManagementClient("localhost:7081");

    public ManagementClientTest() throws Exception {
    }

    @Test
    @DisplayName("测试启动节点")
    void testStartNode() {
        HashMap<String, ManagementClient.NodeOperateStatus> status = managementClient.startNodeList(
                new ArrayList<>(Arrays.asList("121000005l35120456.node1", "12110107bi45jh675g.node2"))
        );
        System.out.println(JSON.toJSONString(status, JSONWriter.Feature.PrettyFormat));
    }

    @Test
    @DisplayName("测试停止节点")
    void testStopNode() {
        HashMap<String, ManagementClient.NodeOperateStatus> status = managementClient.stopNodeList(
                new ArrayList<>(Arrays.asList("12110107bi45jh675g.node2", "122000002n00123567.node3"))
        );
        System.out.println(JSON.toJSONString(status, JSONWriter.Feature.PrettyFormat));
    }

    @Test
    @DisplayName("测试查询节点网络ID")
    void testQueryNodeNetWorkId() {
        HashMap<String, String> status = managementClient.queryNetWorkIdByNodeNameList(
                new ArrayList<>(Arrays.asList("121000005l35120456.node1", "122000002n00123567.node3"))
        );
        System.out.println(JSON.toJSONString(status, JSONWriter.Feature.PrettyFormat));
    }

    @Test
    @DisplayName("测试查询节点状态")
    void testQueryNodeStatus() {
        HashMap<String, ManagementClient.NodeStatus> status = managementClient.queryNodeStatusByNodeNameList(
                new ArrayList<>(Arrays.asList("121000005l35120456.node1", "12110107bi45jh675g.node2"))
        );
        System.out.println(JSON.toJSONString(status, JSONWriter.Feature.PrettyFormat));
    }


}
