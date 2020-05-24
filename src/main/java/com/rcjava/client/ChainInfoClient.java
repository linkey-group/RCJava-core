package com.rcjava.client;

import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.rcjava.protos.Peer.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author zyf
 */
public class ChainInfoClient {

    private String host;
    private boolean useJavaImpl = false;

    private BaseClient rClient = new RClient();
    private BaseClient rcJavaClient = new RCJavaClient();

    private BaseClient client = rClient;

    public ChainInfoClient(String host) {
        this.host = host;
    }

    public ChainInfoClient(String host, boolean useJavaImpl) {
        this.host = host;
        this.useJavaImpl = useJavaImpl;
        this.client = useJavaImpl ? rcJavaClient : rClient;
    }

    /**
     * 获取区块链信息
     *
     * @return 当前区块链高度、总共交易数、currentBlockHash、previousBlockHash等信息
     */
    public BlockchainInfo getChainInfo() {
        JSONObject result = client.getJObject("http://" + host + "/chaininfo");
        BlockchainInfo.Builder chainInfoBuilder = BlockchainInfo.newBuilder();
        String json = result.toJSONString();
        try {
            JsonFormat.parser().merge(json, chainInfoBuilder);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        BlockchainInfo blockchainInfo = chainInfoBuilder.build();
        return blockchainInfo;
    }

    /**
     * 获取总节点数以及共识节点数
     *
     * @return
     */
    public NodesNum getChainInfoNode() {
        JSONObject jsonObject = client.getJObject("http://" + host + "/chaininfo/node");
        NodesNum nodesNum = new NodesNum(jsonObject.getInteger("consensusnodes"), jsonObject.getInteger("nodes"));
        return nodesNum;
    }


    /**
     * 根据高度获取块
     *
     * @param height 区块高度
     * @return Block
     */
    public Block getBlockByHeight(long height) {
        // 根据高度获取块数据
        JSONObject result = client.getJObject("http://" + host + "/block/" + height);
        return genBlockFromJobject(result);
    }

    /**
     * 根据高度获取块（流式获取）
     *
     * @param height 区块高度
     * @return 返回对应高度的块（流式获取）
     */
    public Block getBlockStreamByHeight(long height) {
        InputStream inputStream = client.getInputStream("http://" + host + "/block/stream/" + height);
        Block block = Block.getDefaultInstance();
        try {
            block = Block.parseFrom(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return block;
    }

    /**
     * 根据blockHash获取block
     *
     * @param blockHash Block的hash值,Base64字符串
     * @return Block 返回检索到的块
     */
    public Block getBlockByBlockHash(String blockHash) {
        JSONObject result = client.getJObject("http://" + host + "/block/hash/" + blockHash);
        return genBlockFromJobject(result);
    }

    /**
     * 从请求返回来的JsonObject构建Block
     *
     * @param jsonObject
     * @return
     */
    private Block genBlockFromJobject(JSONObject jsonObject) {
        if (jsonObject == null || jsonObject.isEmpty()) {
            return null;
        }
        String json = jsonObject.toJSONString();
        Block.Builder builder = Block.newBuilder();
        try {
            JsonFormat.parser().merge(json, builder);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        return builder.build();
    }

    /**
     * 根据blockHash获取block
     *
     * @param blockHash Block的hash值
     * @return Block 返回检索到的块
     */
    public Block getBlockByBlockHash(ByteString blockHash) {
        String hash = Base64.encodeBase64String(blockHash.toByteArray());
        Block block = getBlockByBlockHash(hash);
        return block;
    }

    /**
     * 根据tranId查询交易
     *
     * @param tranId tranId
     * @return 返回检索到的交易
     */
    public Transaction getTranByTranId(String tranId) {
        JSONObject result = client.getJObject("http://" + host + "/transaction/" + tranId);
        if (result == null || result.isEmpty()) {
            return null;
        }
        Transaction.Builder builder = Transaction.newBuilder();
        try {
            String json = result.toJSONString();
            JsonFormat.parser().merge(json, builder);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        return builder.build();
    }

    /**
     * 通过tranId查询交易（流式获取）
     *
     * @param tranId 交易ID
     * @return 返回检索到的交易（流式获取）
     */
    public Transaction getTranStreamByTranId(String tranId) {
        InputStream inputStream = client.getInputStream("http://" + host + "/transaction/stream/" + tranId);
        Transaction transaction = Transaction.getDefaultInstance();
        try {
            transaction = Transaction.parseFrom(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return transaction;
    }

    /**
     * 根据tranId查询交易信息和所在区块高度
     *
     * @param tranId 交易tranId
     * @return 返回检索到的交易
     */
    public TranInfoAndHeight getTranInfoAndHeightByTranId(String tranId) {
        JSONObject result = client.getJObject("http://" + host + "/transaction/tranInfoAndHeight/" + tranId);
        if (result == null || result.isEmpty()) {
            return null;
        }
        Transaction.Builder tranBuilder = Transaction.newBuilder();
        try {
            String json = result.getString("tranInfo");
            JsonFormat.parser().merge(json, tranBuilder);

        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        Long height = result.getLong("height");
        TranInfoAndHeight tranInfoAndHeight = new TranInfoAndHeight(tranBuilder.build(), height);
        return tranInfoAndHeight;
    }

    // TODO 通过post获取相关信息


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

    /**
     * 共识节点与总节点的数量
     */
    public class NodesNum {

        private Integer consensusNodes;
        private Integer nodes;

        public NodesNum(Integer consensusNodes, Integer nodes) {
            this.consensusNodes = consensusNodes;
            this.nodes = nodes;
        }

        public Integer getConsensusNodes() {
            return consensusNodes;
        }

        public Integer getNodes() {
            return nodes;
        }
    }

    /**
     * 内部类TranInfoAndHeight
     */
    public class TranInfoAndHeight {

        private Transaction tranInfo;
        private Long height;

        public TranInfoAndHeight(Transaction tranInfo, Long height) {
            this.tranInfo = tranInfo;
            this.height = height;
        }

        public Transaction getTranInfo() {
            return tranInfo;
        }

        public Long getHeight() {
            return height;
        }
    }
}

