/**
 * Copyright  2024 Linkel Technology Co., Ltd, Beijing.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BA SIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rcjava.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContexts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Created by 北京连琪科技有限公司.
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

    @Test
    @DisplayName("循环测试启动停止节点")
    void testOperateNode() {
        String[] nodeNames = new String[]{"122000002n00123567.node3", "921000005k36123789.node4", "921000006e0012v696.node5"};
        while (true) {
            try {
                int random = new Random().nextInt(3);
                String nodeName = nodeNames[random];
                HashMap<String, ManagementClient.NodeOperateStatus> stopStatus = managementClient.stopNodeList(new ArrayList<>(Collections.singletonList(nodeName)));
                System.out.println(JSON.toJSONString(stopStatus.get(nodeName)));
                TimeUnit.SECONDS.sleep(45);

//                HashMap<String, ManagementClient.NodeStatus> queryStatus = new HashMap<>();
//                queryStatus.put("nodeName", new ManagementClient.NodeStatus(200, "Starting", ""));
//
//                queryStatus = managementClient.queryNodeStatusByNodeNameList(new ArrayList<>(Collections.singletonList(nodeName)));
//                TimeUnit.SECONDS.sleep(5);

                HashMap<String, ManagementClient.NodeOperateStatus> startStatus = managementClient.startNodeList(new ArrayList<>(Collections.singletonList(nodeName)));
                System.out.println(JSON.toJSONString(startStatus.get(nodeName)));
                // 验证启动结果
                assertEquals(200, startStatus.get(nodeName).getCode(), "节点启动应该返回200状态码");

                TimeUnit.SECONDS.sleep(45);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }

    }


}
