package com.rcjava.client;

import com.alibaba.fastjson2.JSONObject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.rcjava.protos.Peer.*;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * @author zyf
 */
public class ChainInfoClient {

    private String host;
    private boolean useSsl = false;
    private boolean useJavaImpl = false;

    private SSLContext sslContext;

    private final String PROTOCOL_HTTP = "http";
    private final String PROTOCOL_HTTPS = "https";

    private BaseClient rClient = new RClient();
    private BaseClient rcJavaClient = new RCJavaClient();

    private BaseClient client = rClient;

    public ChainInfoClient(String host) {
        this.host = host;
    }

    public ChainInfoClient(String host, SSLContext sslContext) {
        this.host = host;
        this.sslContext = sslContext;
        this.rClient = new RClient(sslContext);
        this.rcJavaClient = new RCJavaClient(sslContext);
        this.client = rClient;
        this.useSsl = true;
    }

    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 获取区块链信息
     *
     * @return 当前区块链高度、总共交易数、currentBlockHash、previousBlockHash等信息
     */
    public BlockchainInfo getChainInfo() {
        String protocol = useSsl ? PROTOCOL_HTTPS : PROTOCOL_HTTP;
        String url = String.format("%s://%s/chaininfo", protocol, host);
        JSONObject result = client.getJObject(url);
        BlockchainInfo.Builder chainInfoBuilder = BlockchainInfo.newBuilder();
        String json = result.toJSONString();
        try {
            JsonFormat.parser().merge(json, chainInfoBuilder);
        } catch (InvalidProtocolBufferException e) {
            logger.error("construct ChainInfo occurs error, errorMsg is {}", e.getMessage(), e);
        }
        BlockchainInfo blockchainInfo = chainInfoBuilder.build();
        return blockchainInfo;
    }

    /**
     * Created by 北京连琪科技有限公司.
     * 获取总节点数以及共识节点数
     *
     * @return
     */
    public NodesNum getChainInfoNode() {
        String protocol = useSsl ? PROTOCOL_HTTPS : PROTOCOL_HTTP;
        String url = String.format("%s://%s/chaininfo/node", protocol, host);
        JSONObject jsonObject = client.getJObject(url);
        NodesNum nodesNum = new NodesNum(jsonObject.getInteger("consensusnodes"), jsonObject.getInteger("nodes"));
        return nodesNum;
    }

    public AllNodesInfo getAllNodeInfo() {
        String protocol = useSsl ? PROTOCOL_HTTPS : PROTOCOL_HTTP;
        String url = String.format("%s://%s/chaininfo/nodesinfo", protocol, host);
        JSONObject jsonObject = client.getJObject(url);
        AllNodesInfo allNodesInfo = jsonObject.to(AllNodesInfo.class);
        return allNodesInfo;
    }


    /**
     * 根据高度获取块
     *
     * @param height 区块高度
     * @return Block
     */
    public Block getBlockByHeight(long height) {
        String protocol = useSsl ? PROTOCOL_HTTPS : PROTOCOL_HTTP;
        // 根据高度获取块数据
        String url = String.format("%s://%s/block/%s", protocol, host, height);
        JSONObject result = client.getJObject(url);
        return genBlockFromJobject(result);
    }

    /**
     * 根据高度获取块（流式获取）
     *
     * @param height 区块高度
     * @return 返回对应高度的块（流式获取）
     */
    public Block getBlockStreamByHeight(long height) {
        String protocol = useSsl ? PROTOCOL_HTTPS : PROTOCOL_HTTP;
        String url = String.format("%s://%s/block/stream/%s", protocol, host, height);
        InputStream inputStream = client.getInputStream(url);
        Block block = Block.getDefaultInstance();
        try {
            block = Block.parseFrom(inputStream);
        } catch (IOException e) {
            logger.error("construct Block occurs error, errorMsg is {}", e.getMessage(), e);
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
     * 根据高度获取区块头
     *
     * @param height 区块高度
     * @return BlockHeader
     */
    public BlockHeader getBlockHeaderByHeight(long height) {
        String protocol = useSsl ? PROTOCOL_HTTPS : PROTOCOL_HTTP;
        // 根据高度获取块数据
        String url = String.format("%s://%s/block/header/%s", protocol, host, height);
        JSONObject result = client.getJObject(url);
        return genBlockHeaderFromJobject(result);
    }

    /**
     * 根据blockHash获取block
     *
     * @param blockHash Block的hash值,Base64字符串
     * @return Block 返回检索到的块
     */
    public Block getBlockByBlockHash(String blockHash) {
        String protocol = useSsl ? PROTOCOL_HTTPS : PROTOCOL_HTTP;
        String url = String.format("%s://%s/block/hash/%s", protocol, host, blockHash);
        JSONObject result = client.getJObject(url);
        return genBlockFromJobject(result);
    }

    /**
     * 根据blockHash获取blockHeader
     *
     * @param blockHash Block的hash值,Base64字符串
     * @return BlockHeader 返回检索到的区块头
     */
    public BlockHeader getBlockHeaderByBlockHash(String blockHash) {
        String protocol = useSsl ? PROTOCOL_HTTPS : PROTOCOL_HTTP;
        String url = String.format("%s://%s/block/header/hash/%s", protocol, host, blockHash);
        JSONObject result = client.getJObject(url);
        return genBlockHeaderFromJobject(result);
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
            logger.error("construct Block occurs error, errorMsg is {}, json is {}", e.getMessage(), json, e);
        }
        return builder.build();
    }

    /**
     * 从请求返回来的JsonObject构建BlockHeader
     *
     * @param jsonObject
     * @return
     */
    private BlockHeader genBlockHeaderFromJobject(JSONObject jsonObject) {
        if (jsonObject == null || jsonObject.isEmpty()) {
            return null;
        }
        String json = jsonObject.toJSONString();
        BlockHeader.Builder builder = BlockHeader.newBuilder();
        try {
            JsonFormat.parser().merge(json, builder);
        } catch (InvalidProtocolBufferException e) {
            logger.error("construct BlockHeader occurs error, errorMsg is {}, json is {}", e.getMessage(), json, e);
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
     * 返回交易入块时间
     *
     * @param tranId
     * @return
     */
    public CreateTime getBlockTimeByTranId(String tranId) {
        String protocol = useSsl ? PROTOCOL_HTTPS : PROTOCOL_HTTP;
        String url = String.format("%s://%s/block/blocktimeoftran/%s", protocol, host, tranId);
        JSONObject result = client.getJObject(url);
        if (result == null || result.isEmpty()) {
            return null;
        } else {
            CreateTime createTime = new CreateTime(result.getString("createTime"), result.getString("createTimeUtc"));
            return createTime;
        }
    }

    /**
     * 根据tranId查询交易
     *
     * @param tranId tranId
     * @return 返回检索到的交易
     */
    public Transaction getTranByTranId(String tranId) {
        String protocol = useSsl ? PROTOCOL_HTTPS : PROTOCOL_HTTP;
        String url = String.format("%s://%s/transaction/%s", protocol, host, tranId);
        JSONObject result = client.getJObject(url);
        if (result == null || result.isEmpty()) {
            return null;
        }
        Transaction.Builder builder = Transaction.newBuilder();
        try {
            String json = result.toJSONString();
            JsonFormat.parser().merge(json, builder);
        } catch (InvalidProtocolBufferException e) {
            logger.error("construct Transaction occurs error, errorMsg is {}", e.getMessage(), e);
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
        String protocol = useSsl ? PROTOCOL_HTTPS : PROTOCOL_HTTP;
        String url = String.format("%s://%s/transaction/stream/%s", protocol, host, tranId);
        InputStream inputStream = client.getInputStream(url);
        Transaction transaction = null;
        try {
            transaction = Transaction.parseFrom(inputStream);
        } catch (IOException e) {
            logger.error("construct Transaction occurs error, errorMsg is {}", e.getMessage(), e);
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
        String protocol = useSsl ? PROTOCOL_HTTPS : PROTOCOL_HTTP;
        String url = String.format("%s://%s/transaction/tranInfoAndHeight/%s", protocol, host, tranId);
        JSONObject result = client.getJObject(url);
        if (result == null || result.isEmpty()) {
            return null;
        }
        Transaction.Builder tranBuilder = Transaction.newBuilder();
        try {
            String json = result.getString("tranInfo");
            JsonFormat.parser().merge(json, tranBuilder);
        } catch (InvalidProtocolBufferException e) {
            logger.error("construct Transaction occurs error, errorMsg is {}", e.getMessage(), e);
        }
        Long height = result.getLong("height");
        TranInfoAndHeight tranInfoAndHeight = new TranInfoAndHeight(tranBuilder.build(), height);
        return tranInfoAndHeight;
    }

    /**
     * 根据交易ID获取对应的交易结果
     *
     * @param tranId 交易ID
     * @return 返回本条交易的执行结果
     */
    public TransactionResult getTranResultByTranId(String tranId) {
        return getTranAndResultByTranId(tranId).getTranResult();
    }

    /**
     * 根据交易ID获取对应的交易内容以及其执行结果
     *
     * @param tranId 交易ID
     * @return 返回本条交易内容与执行结果
     */
    public TranAndTranResult getTranAndResultByTranId(String tranId) {
        TranInfoAndHeight infoAndHeight = getTranInfoAndHeightByTranId(tranId);
        if (infoAndHeight == null) {
            return null;
        }
        Transaction tran = infoAndHeight.getTranInfo();
        Block block = getBlockByHeight(infoAndHeight.getHeight());
        return new TranAndTranResult(tran, block.getTransactionResultsList().stream()
                .filter(tranResult -> tranResult.getTxId().equals(tranId))
                .findAny()
                .orElse(null));
    }

    // TODO 通过post获取相关信息

    /**
     * 查询RepChain的leveldb/rocksdb中的数据
     *
     * @param netId         网络ID
     * @param chainCodeName 合约名
     * @param oid           交易实例ID
     * @param key           数据库中的key
     * @return
     */
    public Object queryDB(String netId, String chainCodeName, String oid, String key) {
        String protocol = useSsl ? PROTOCOL_HTTPS : PROTOCOL_HTTP;
        String url = String.format("%s://%s/db/query", protocol, host);
        JSONObject query = new JSONObject();
        query.fluentPut("netId", netId);
        query.fluentPut("chainCodeName", chainCodeName);
        query.fluentPut("oid", oid);
        query.fluentPut("key", key);
        JSONObject result = client.postJString(url, query.toJSONString());
        if (result == null || result.isEmpty()) {
            return null;
        } else {
            return result.get("result");
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
     * 交易入块时间
     */
    public static class CreateTime {

        private final String createTime;
        private final String createTimeUtc;

        public CreateTime(String createTime, String createTimeUtc) {
            this.createTime = createTime;
            this.createTimeUtc = createTimeUtc;
        }

        public String getCreateTime() {
            return createTime;
        }

        public String getCreateTimeUtc() {
            return createTimeUtc;
        }
    }

    /**
     * 共识节点与总节点的数量
     */
    public static class NodesNum {

        private final Integer consensusNodes;
        private final Integer nodes;

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
     * Created by 北京连琪科技有限公司.
     */
    public static class NodesInfo {
        private String ip;
        private String p2p_port;
        private String nodeName;

        public NodesInfo() {
        }

        public NodesInfo(String ip, String p2p_port, String nodeName) {
            this.ip = ip;
            this.p2p_port = p2p_port;
            this.nodeName = nodeName;
        }

        public String getIp() {
            return ip;
        }

        public String getP2p_port() {
            return p2p_port;
        }

        public String getNodeName() {
            return nodeName;
        }
    }

    /**
     * Created by 北京连琪科技有限公司.
     */
    public static class AllNodesInfo {
        private ArrayList<NodesInfo> consensusNodes;
        private ArrayList<NodesInfo> nodes;

        public AllNodesInfo(ArrayList<NodesInfo> consensusNodes, ArrayList<NodesInfo> nodes) {
            this.consensusNodes = consensusNodes;
            this.nodes = nodes;
        }

        public ArrayList<NodesInfo> getConsensusNodes() {
            return consensusNodes;
        }

        public ArrayList<NodesInfo> getNodes() {
            return nodes;
        }
    }

    /**
     * 内部类TranInfoAndHeight
     */
    public static class TranInfoAndHeight {

        private final Transaction tranInfo;
        private final Long height;

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

    /**
     * 内部类TranAndTranResult
     */
    public static class TranAndTranResult {

        private final Transaction tran;
        private final TransactionResult tranResult;

        public TranAndTranResult(Transaction tran, TransactionResult tranResult) {
            this.tran = tran;
            this.tranResult = tranResult;
        }

        public Transaction getTran() {
            return tran;
        }

        public TransactionResult getTranResult() {
            return tranResult;
        }
    }
}

