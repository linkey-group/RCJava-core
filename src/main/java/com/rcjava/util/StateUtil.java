package com.rcjava.util;

import com.rcjava.client.ChainInfoClient;
import com.rcjava.protos.Peer;
import com.twitter.chill.KryoInjection;
import org.json4s.Extraction;
import org.json4s.Formats;
import org.json4s.jackson.JsonMethods;
import org.json4s.jackson.Serialization;


/**
 * 对RepChain中key-value中的value进行反序列化
 *
 * @author zyf
 */
public class StateUtil {

    private static JsonMethods jsonMethods = org.json4s.jackson.JsonMethods$.MODULE$;
    private static Formats formats = (Formats) Serialization.formats(org.json4s.NoTypeHints$.MODULE$);

    /**
     * 反序列化为Json
     *
     * @param bytes value值，使用protobuf中的toByteArray()得到
     * @return
     */
    public static String toJsonString(byte[] bytes) {
        Object object = KryoInjection.invert(bytes).getOrElse(null);
//        String jsonStr = Serialization.write(object, formats);
        return jsonMethods.compact(jsonMethods.render(Extraction.decompose(object, formats), formats));
    }

    /**
     * 反序列化为具体的实例
     *
     * @param bytes value值，使用protobuf中的toByteArray()得到
     * @return
     */
    public static Object toInstance(byte[] bytes) {
        return KryoInjection.invert(bytes).getOrElse(null);
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
