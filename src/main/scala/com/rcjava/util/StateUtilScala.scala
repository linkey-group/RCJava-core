package com.rcjava.util

import com.rcjava.client.ChainInfoClient
import com.twitter.chill.KryoInjection
import org.json4s.{ Formats, NoTypeHints }
import org.json4s.jackson.{ JsonMethods, Serialization }
import org.slf4j.{ Logger, LoggerFactory }

import scala.reflect.ClassTag
import scala.util.{ Failure, Success, Try }

/**
 * 对 RepChain 链上 K-V 中的 value 进行反序列化。
 *
 * 对应 Scala 端的 rep.utils.SerializeUtils。
 *
 * ─────────────────────── 核心原理 ─────────────────────────────────
 *
 * RepChain 侧：
 *   合约执行后将 Scala protobuf 对象（rep.proto.rc2.*，由 ScalaPB 生成）
 *   通过 chill-kryo（KryoInjection.apply）序列化为字节数组并写入链上 K-V。
 *
 * rcjava 侧（本工具类）：
 *   1. 从链上读取字节数组（getStatesSetMap / getStatesGetMap 的 .toByteArray()）
 *   2. 用 KryoInjection.invert(bytes) 还原为 JVM 对象。
 *      ─ 关键前提：classpath 中必须有与 RepChain 侧完全相同的 rep.proto.rc2.* 类，
 *        否则 Kryo 找不到类定义，反序列化将失败。
 *        这些类由 src/main/protobuf/rc2.proto 经 ScalaPB 生成，
 *        位于 src/main/scala/rep/proto/rc2/。
 *   3. 将还原后的对象转为 JSON 字符串或强类型实例（通过 Scala ClassTag）。
 *
 * ──────────────────────────────────────────────────────────────────
 *
 * 用法举例：
 * {{{
 *   // 按 JSON 字符串读取
 *   val json: Option[String] = StateUtil.toJsonString(bytes)
 *
 *   // 按类型读取（rep.proto.rc2.Signer 由 ScalaPB 生成）
 *   val signer: Option[rep.proto.rc2.Signer] =
 *     StateUtil.toInstance[rep.proto.rc2.Signer](bytes)
 *
 *   // 从链上一步到位
 *   val result = StateUtil.fromChain("localhost:9081", "txId", "contractKey")
 * }}}
 *
 * @author zyf (Java → Scala 迁移 & 功能增强)
 */
object StateUtilScala {

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  // json4s 格式（与 rep.utils.SerializeUtils 保持一致）
  private implicit val formats: Formats = Serialization.formats(NoTypeHints)

  private val jsonMethods: JsonMethods.type = JsonMethods

  // ─────────────────────── 公开 API ────────────────────────────────

  /**
   * 将 Kryo 序列化字节数组反序列化为 JSON 字符串。
   * 对应 Scala 端的 rep.utils.SerializeUtils#compactJson。
   *
   * @param bytes 链上 value 字节数组（由 protobuf 的 toByteArray 得到）
   * @return Some(jsonStr) 或 None（失败时记录日志）
   */
  def toJsonString(bytes: Array[Byte]): Option[String] = {
    if (bytes == null || bytes.isEmpty) return None

    fromKryo(bytes).flatMap { obj =>
      Try {
        import org.json4s.Extraction
        jsonMethods.compact(
          jsonMethods.render(Extraction.decompose(obj))
        )
      } match {
        case Success(json) => Some(json)
        case Failure(e) =>
          logger.error(s"JSON 转换失败: ${e.getMessage}", e)
          None
      }
    }
  }

  /**
   * 将 Kryo 序列化字节数组反序列化为原始 JVM 对象。
   * 对应 Scala 端的 rep.utils.SerializeUtils#deserialise。
   *
   * @param bytes 链上 value 字节数组
   * @return Some(obj) 或 None
   */
  def toInstance(bytes: Array[Byte]): Option[Any] =
    if (bytes == null || bytes.isEmpty) None
    else fromKryo(bytes)

  /**
   * 将 Kryo 序列化字节数组反序列化为指定 Scala/Java 类型。
   *
   * 典型用法（链上 Signer 对象）：
   * {{{
   *   val signer = StateUtil.toInstance[rep.proto.rc2.Signer](bytes)
   * }}}
   *
   * @param bytes 链上 value 字节数组
   * @tparam T 目标类型（需在 classpath 中，即 ScalaPB 生成的 rep.proto.rc2.* 类）
   * @return Some(T) 或 None（类型不匹配时记录错误并返回 None）
   */
  def toInstance[T: ClassTag](bytes: Array[Byte]): Option[T] = {
    val clazz = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]
    fromKryo(bytes).flatMap {
      case t: T => Some(t)   // Scala ClassTag 检查
      case obj =>
        logger.error(s"类型转换失败，期望: ${clazz.getName}，实际: ${obj.getClass.getName}")
        None
    }
  }

  /**
   * 便捷方法：直接从链上读取指定 K-V 并转为 JSON。
   *
   * @param endpoint   RepChain 节点地址，如 "localhost:9081"
   * @param txId       交易 ID
   * @param stateKey   合约中写入的 Key 值
   * @return Some(jsonStr) 或 None
   */
  def fromChain(endpoint: String, txId: String, stateKey: String): Option[String] = {
    val client = new ChainInfoClient(endpoint)
    val statesSetMap = client.getTranResultByTranId(txId).getStatesSetMap
    if (!statesSetMap.containsKey(stateKey)) {
      logger.warn("链上未找到 key: {}", stateKey)
      None
    } else {
      toJsonString(statesSetMap.get(stateKey).toByteArray)
    }
  }

  // ─────────────────────── 私有工具 ────────────────────────────────

  /** 调用 KryoInjection.invert，统一处理错误日志 */
  private def fromKryo(bytes: Array[Byte]): Option[Any] =
    KryoInjection.invert(bytes) match {
      case Success(obj) => Option(obj)
      case Failure(e) =>
        logger.error(s"Kryo 反序列化失败: ${e.getMessage}", e)
        None
    }

  // ─────────────────────── main（快速验证） ────────────────────────

  def main(args: Array[String]): Unit = {
    val txId     = "这里输入交易ID"
    val stateKey = "这里输入合约中的Key"
    val endpoint = "localhost:9081"

    // 示例 1：转为 JSON
    val kv = fromChain(endpoint, txId, stateKey).get

    // 示例 2：转为 Signer（ScalaPB 生成的 rep.proto.rc2.Signer）
    val client = new ChainInfoClient(endpoint)
    val bytes  = client.getTranResultByTranId(txId)
      .getStatesSetMap
      .get(stateKey)
      .toByteArray
    toInstance[rep.proto.rc2.Signer](bytes).foreach(println)
  }
}
