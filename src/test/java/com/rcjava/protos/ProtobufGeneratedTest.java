package com.rcjava.protos;

import com.google.protobuf.Timestamp;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Protobuf 生成代码测试
 * 验证生成的 Java 类是否正常工作
 * 
 * @author zyf
 */
public class ProtobufGeneratedTest {

    @Test
    public void testCreateTransaction() {
        // 创建一个简单的 Transaction
        Peer.Transaction transaction = Peer.Transaction.newBuilder()
                .setId("test-tx-001")
                .setType(Peer.Transaction.Type.CHAINCODE_INVOKE)
                .setGasLimit(1000)
                .build();

        assertEquals("test-tx-001", transaction.getId());
        assertEquals(Peer.Transaction.Type.CHAINCODE_INVOKE, transaction.getType());
        assertEquals(1000, transaction.getGasLimit());
    }

    @Test
    public void testCreateChaincodeInput() {
        // 创建合约输入
        Peer.ChaincodeInput input = Peer.ChaincodeInput.newBuilder()
                .setFunction("transfer")
                .addArgs("from_address")
                .addArgs("to_address")
                .addArgs("100")
                .build();

        assertEquals("transfer", input.getFunction());
        assertEquals(3, input.getArgsCount());
        assertEquals("from_address", input.getArgs(0));
        assertEquals("to_address", input.getArgs(1));
        assertEquals("100", input.getArgs(2));
    }

    @Test
    public void testCreateBlockHeader() {
        // 创建区块头
        Peer.BlockHeader header = Peer.BlockHeader.newBuilder()
                .setVersion(1)
                .setHeight(100)
                .setHashPresent(com.google.protobuf.ByteString.copyFromUtf8("current_hash"))
                .setHashPrevious(com.google.protobuf.ByteString.copyFromUtf8("previous_hash"))
                .build();

        assertEquals(1, header.getVersion());
        assertEquals(100, header.getHeight());
        assertNotNull(header.getHashPresent());
        assertNotNull(header.getHashPrevious());
    }

    @Test
    public void testCreateBlock() {
        // 创建完整的区块
        Peer.BlockHeader header = Peer.BlockHeader.newBuilder()
                .setVersion(1)
                .setHeight(1)
                .build();

        Peer.Transaction tx1 = Peer.Transaction.newBuilder()
                .setId("tx-001")
                .setType(Peer.Transaction.Type.CHAINCODE_INVOKE)
                .build();

        Peer.Transaction tx2 = Peer.Transaction.newBuilder()
                .setId("tx-002")
                .setType(Peer.Transaction.Type.CHAINCODE_DEPLOY)
                .build();

        Peer.Block block = Peer.Block.newBuilder()
                .setHeader(header)
                .addTransactions(tx1)
                .addTransactions(tx2)
                .build();

        assertEquals(1, block.getHeader().getHeight());
        assertEquals(2, block.getTransactionsCount());
        assertEquals("tx-001", block.getTransactions(0).getId());
        assertEquals("tx-002", block.getTransactions(1).getId());
    }

    @Test
    public void testSerializeAndDeserialize() throws Exception {
        // 测试序列化和反序列化
        Peer.Transaction original = Peer.Transaction.newBuilder()
                .setId("serialize-test")
                .setType(Peer.Transaction.Type.CHAINCODE_INVOKE)
                .setGasLimit(5000)
                .build();

        // 序列化
        byte[] bytes = original.toByteArray();
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        // 反序列化
        Peer.Transaction deserialized = Peer.Transaction.parseFrom(bytes);
        assertEquals(original.getId(), deserialized.getId());
        assertEquals(original.getType(), deserialized.getType());
        assertEquals(original.getGasLimit(), deserialized.getGasLimit());
    }

    @Test
    public void testCreateSigner() {
        // 创建签名者
        Timestamp createTime = Timestamp.newBuilder()
                .setSeconds(System.currentTimeMillis() / 1000)
                .setNanos(0)
                .build();

        Peer.Signer signer = Peer.Signer.newBuilder()
                .setName("测试用户")
                .setCreditCode("91110000000000000X")
                .setMobile("13800138000")
                .addCertNames("cert-001")
                .setSignerValid(true)
                .setVersion("1.0")
                .setCreateTime(createTime)
                .build();

        assertEquals("测试用户", signer.getName());
        assertEquals("91110000000000000X", signer.getCreditCode());
        assertEquals("13800138000", signer.getMobile());
        assertEquals(1, signer.getCertNamesCount());
        assertTrue(signer.getSignerValid());
        assertEquals("1.0", signer.getVersion());
    }

    @Test
    public void testCreateCertificate() {
        // 创建证书
        Peer.CertId certId = Peer.CertId.newBuilder()
                .setCreditCode("91110000000000000X")
                .setCertName("test-cert")
                .setVersion("1.0")
                .build();

        Peer.Certificate certificate = Peer.Certificate.newBuilder()
                .setCertificate("-----BEGIN CERTIFICATE-----\ntest\n-----END CERTIFICATE-----")
                .setAlgType("SHA256withECDSA")
                .setCertValid(true)
                .setCertType(Peer.Certificate.CertType.CERT_AUTHENTICATION)
                .setId(certId)
                .setCertHash("abc123")
                .setVersion("1.0")
                .build();

        assertTrue(certificate.getCertValid());
        assertEquals("SHA256withECDSA", certificate.getAlgType());
        assertEquals(Peer.Certificate.CertType.CERT_AUTHENTICATION, certificate.getCertType());
        assertEquals("test-cert", certificate.getId().getCertName());
    }

    @Test
    public void testCreateChaincodeDeploy() {
        // 创建合约部署信息
        Peer.ChaincodeDeploy deploy = Peer.ChaincodeDeploy.newBuilder()
                .setTimeout(30000)
                .setCodePackage("contract code here")
                .setCType(Peer.ChaincodeDeploy.CodeType.CODE_SCALA)
                .setRType(Peer.ChaincodeDeploy.RunType.RUN_SERIAL)
                .setSType(Peer.ChaincodeDeploy.StateType.STATE_GLOBAL)
                .setInitParameter("{\"param\":\"value\"}")
                .setCclassification(Peer.ChaincodeDeploy.ContractClassification.CONTRACT_CUSTOM)
                .setIsCalledByOtherContracts(false)
                .build();

        assertEquals(30000, deploy.getTimeout());
        assertEquals(Peer.ChaincodeDeploy.CodeType.CODE_SCALA, deploy.getCType());
        assertEquals(Peer.ChaincodeDeploy.RunType.RUN_SERIAL, deploy.getRType());
        assertFalse(deploy.getIsCalledByOtherContracts());
    }

    @Test
    public void testTransactionResult() {
        // 创建交易结果
        Peer.ActionResult actionResult = Peer.ActionResult.newBuilder()
                .setCode(0)
                .setReason("success")
                .setData("Transaction executed successfully")
                .build();

        Peer.TransactionResult txResult = Peer.TransactionResult.newBuilder()
                .setTxId("tx-result-001")
                .putStatesGet("key1", com.google.protobuf.ByteString.copyFromUtf8("value1"))
                .putStatesSet("key2", com.google.protobuf.ByteString.copyFromUtf8("value2"))
                .setErr(actionResult)
                .build();

        assertEquals("tx-result-001", txResult.getTxId());
        assertEquals(1, txResult.getStatesGetCount());
        assertEquals(1, txResult.getStatesSetCount());
        assertEquals(0, txResult.getErr().getCode());
        assertEquals("success", txResult.getErr().getReason());
    }
}

