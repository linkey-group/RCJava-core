package com.rcjava.util;

import com.rcjava.client.ChainInfoClient;
import com.rcjava.protos.Peer;
import com.twitter.chill.KryoInjection;
import org.json4s.Extraction;
import org.json4s.Formats;
import org.json4s.jackson.JsonMethods;
import org.json4s.jackson.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.util.Try;


/**
 * 对RepChain中key-value中的value进行反序列化
 * 对应 Scala 端的 rep.utils.SerializeUtils
 *
 * @author zyf
 */
public class StateUtil {

    private static JsonMethods jsonMethods = org.json4s.jackson.JsonMethods$.MODULE$;
    private static Formats formats = (Formats) Serialization.formats(org.json4s.NoTypeHints$.MODULE$);

    private static Logger logger = LoggerFactory.getLogger(StateUtil.class);

    /**
     * 反序列化为Json（对应 Scala 端的 compactJson 方法）
     *
     * @param bytes value值，使用protobuf中的toByteArray()得到
     * @return JSON字符串，如果反序列化失败返回null
     */
    public static String toJsonString(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        
        Try<Object> tryObject = KryoInjection.invert(bytes);
        
        // 匹配 Scala 端的错误处理逻辑
        if (tryObject.isFailure()) {
            logger.error("Kryo反序列化失败: {}", tryObject.failed().get().getMessage());
            return null;
        }
        
        Object object = tryObject.get();
        if (object == null) {
            return null;
        }
        
        try {
            return jsonMethods.compact(jsonMethods.render(Extraction.decompose(object, formats), formats));
        } catch (Exception e) {
            logger.error("JSON转换失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 反序列化为具体的实例（对应 Scala 端的 deserialise 方法）
     *
     * @param bytes value值，使用protobuf中的toByteArray()得到
     * @return 反序列化后的对象，如果失败返回null
     */
    public static Object toInstance(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        
        Try<Object> tryObject = KryoInjection.invert(bytes);
        
        // 匹配 Scala 端的错误处理逻辑
        if (tryObject.isFailure()) {
            logger.error("Kryo反序列化失败: {}", tryObject.failed().get().getMessage());
            return null;
        }

        return tryObject.get();
    }

    /**
     * 反序列化为指定类型的实例（带类型转换）
     *
     * @param bytes value值，使用protobuf中的toByteArray()得到
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 反序列化后的对象，如果失败或类型不匹配返回null
     */
    @SuppressWarnings("unchecked")
    public static <T> T toInstance(byte[] bytes, Class<T> clazz) {
        Object obj = toInstance(bytes);
        if (obj == null) {
            return null;
        }
        
        try {
            return (T) obj;
        } catch (ClassCastException e) {
            logger.error("类型转换失败，期望类型: {}, 实际类型: {}", clazz.getName(), obj.getClass().getName());
            return null;
        }
    }

    public static void main(String[] args) {
        ChainInfoClient chainInfoClient = new ChainInfoClient("localhost:9086");
        byte[] bytes = chainInfoClient.getTranResultByTranId("这里输入交易ID").getStatesSetMap().get("这里输入合约中的Key").toByteArray();
        Peer.Transaction tran = chainInfoClient.getTranAndResultByTranId("这里输入交易ID").getTran();
        String json = toJsonString(bytes);
        Object object = toInstance(bytes);
        System.out.println(json);
        System.out.println(object);
        System.out.println(tran.getIpt().getArgs(0));
    }

}
