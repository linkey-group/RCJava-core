package com.rcjava.client;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * @author zyf
 */
public class ManagementClient {

    private String host;
    private boolean useSsl = false;
    private boolean useJavaImpl = false;

    private SSLContext sslContext;

    private final String PROTOCOL_HTTP = "http";
    private final String PROTOCOL_HTTPS = "https";

    private BaseClient rClient = new RClient();
    private BaseClient rcJavaClient = new RCJavaClient();

    private BaseClient client = rClient;

    public ManagementClient(String host) {
        this.host = host;
    }

    public ManagementClient(String host, SSLContext sslContext) {
        this.host = host;
        this.sslContext = sslContext;
        this.rClient = new RClient(sslContext);
        this.rcJavaClient = new RCJavaClient(sslContext);
        this.client = rClient;
        this.useSsl = true;
    }

    private Logger logger = LoggerFactory.getLogger(getClass());


    /**
     * 启动节点
     *
     * @param nodeNameList
     */
    public HashMap<String, NodeOperateStatus> startNodeList(ArrayList<String> nodeNameList) {
        if (nodeNameList == null || nodeNameList.isEmpty()) {
            return new HashMap<>();
        }

        try {
            String protocol = useSsl ? PROTOCOL_HTTPS : PROTOCOL_HTTP;

            // 使用 URLEncoder 对参数进行编码
            String encodedNodes = nodeNameList.stream()
                    .map(node -> {
                        try {
                            return URLEncoder.encode(node, StandardCharsets.UTF_8.name());
                        } catch (UnsupportedEncodingException e) {
                            logger.error("Failed to encode node name: " + node, e);
                            return node;
                        }
                    })
                    .collect(Collectors.joining(","));

            //String encodedNodes = String.join(",", nodeNameList);
            String url = String.format("%s://%s/management/system/SystemStartup/%s",
                    protocol,
                    host,
                    encodedNodes);

            JSONObject result = client.getJObject(url);
            if (result == null || result.isEmpty()) {
                return new HashMap<>();
            }

            // 指定具体的类型转换
            TypeReference<HashMap<String, NodeStatus>> typeRef =
                    new TypeReference<HashMap<String, NodeStatus>>() {
                    };
            return result.to(typeRef);

        } catch (Exception e) {
            logger.error("Failed to get node status: ", e);
            return new HashMap<>();
        }
    }

    /**
     * 停止节点
     *
     * @param nodeNameList
     */
    public HashMap<String, NodeOperateStatus> stopNodeList(ArrayList<String> nodeNameList) {
        if (nodeNameList == null || nodeNameList.isEmpty()) {
            return new HashMap<>();
        }

        try {
            String protocol = useSsl ? PROTOCOL_HTTPS : PROTOCOL_HTTP;

            // 使用 URLEncoder 对参数进行编码
            String encodedNodes = nodeNameList.stream()
                    .map(node -> {
                        try {
                            return URLEncoder.encode(node, StandardCharsets.UTF_8.name());
                        } catch (UnsupportedEncodingException e) {
                            logger.error("Failed to encode node name: " + node, e);
                            return node;
                        }
                    })
                    .collect(Collectors.joining(","));

            //String encodedNodes = String.join(",", nodeNameList);
            String url = String.format("%s://%s/management/system/SystemStop/%s",
                    protocol,
                    host,
                    encodedNodes);

            JSONObject result = client.getJObject(url);
            if (result == null || result.isEmpty()) {
                return new HashMap<>();
            }

            // 指定具体的类型转换
            TypeReference<HashMap<String, NodeStatus>> typeRef =
                    new TypeReference<HashMap<String, NodeStatus>>() {
                    };
            return result.to(typeRef);

        } catch (Exception e) {
            logger.error("Failed to get node status: ", e);
            return new HashMap<>();
        }
    }

    /**
     * 查询节点对应的网络ID
     *
     * @param nodeNameList
     * @return
     */
    public HashMap<String, String> queryNetWorkIdByNodeNameList(ArrayList<String> nodeNameList) {
        if (nodeNameList == null || nodeNameList.isEmpty()) {
            return new HashMap<>();
        }

        try {
            String protocol = useSsl ? PROTOCOL_HTTPS : PROTOCOL_HTTP;

            // 使用 URLEncoder 对参数进行编码
            String encodedNodes = nodeNameList.stream()
                    .map(node -> {
                        try {
                            return URLEncoder.encode(node, StandardCharsets.UTF_8.name());
                        } catch (UnsupportedEncodingException e) {
                            logger.error("Failed to encode node name: " + node, e);
                            return node;
                        }
                    })
                    .collect(Collectors.joining(","));

            //String encodedNodes = String.join(",", nodeNameList);
            String url = String.format("%s://%s/management/system/SystemNetwork/%s",
                    protocol,
                    host,
                    encodedNodes);

            JSONObject result = client.getJObject(url);
            if (result == null || result.isEmpty()) {
                return new HashMap<>();
            }

            // 指定具体的类型转换
            TypeReference<HashMap<String, String>> typeRef = new TypeReference<HashMap<String, String>>() {
            };
            return result.to(typeRef);

        } catch (Exception e) {
            logger.error("Failed to get node status: ", e);
            return new HashMap<>();
        }
    }

    /**
     * 查询节点状态
     *
     * @param nodeNameList 节点名列表
     * @return
     */
    public HashMap<String, NodeStatus> queryNodeStatusByNodeNameList(ArrayList<String> nodeNameList) {
        if (nodeNameList == null || nodeNameList.isEmpty()) {
            return new HashMap<>();
        }

        try {
            String protocol = useSsl ? PROTOCOL_HTTPS : PROTOCOL_HTTP;

            // 使用 URLEncoder 对参数进行编码
            String encodedNodes = nodeNameList.stream()
                    .map(node -> {
                        try {
                            return URLEncoder.encode(node, StandardCharsets.UTF_8.name());
                        } catch (UnsupportedEncodingException e) {
                            logger.error("Failed to encode node name: " + node, e);
                            return node;
                        }
                    })
                    .collect(Collectors.joining(","));

            //String encodedNodes = String.join(",", nodeNameList);
            String url = String.format("%s://%s/management/system/SystemStatus/%s",
                    protocol,
                    host,
                    encodedNodes);

            JSONObject result = client.getJObject(url);
            if (result == null || result.isEmpty()) {
                return new HashMap<>();
            }

            // 指定具体的类型转换
            TypeReference<HashMap<String, NodeStatus>> typeRef =
                    new TypeReference<HashMap<String, NodeStatus>>() {
                    };
            return result.to(typeRef);

        } catch (Exception e) {
            logger.error("Failed to get node status: ", e);
            return new HashMap<>();
        }
    }


    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public boolean isUseJavaImpl() {
        return useJavaImpl;
    }

    public void setUseJavaImpl(boolean useJavaImpl) {
        this.useJavaImpl = useJavaImpl;
        this.client = useJavaImpl ? rcJavaClient : rClient;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    /**
     * 内部类TranInfoAndHeight
     */
    public static class NodeStatus {
        private final Integer code;
        private final String status;
        private final String desc;

        public NodeStatus(Integer code, String status, String desc) {
            this.code = code;
            this.status = status;
            this.desc = desc;
        }

        public Integer getCode() {
            return code;
        }

        public String getStatus() {
            return status;
        }

        public String getDesc() {
            return desc;
        }
    }

    public static class NodeOperateStatus {
        private final Integer code;
        private final String status;
        private final String desc;

        public NodeOperateStatus(Integer code, String status, String desc) {
            this.code = code;
            this.status = status;
            this.desc = desc;
        }

        public Integer getCode() {
            return code;
        }

        public String getStatus() {
            return status;
        }

        public String getDesc() {
            return desc;
        }
    }
}

