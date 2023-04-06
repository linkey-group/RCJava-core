import org.json4s._
import org.json4s.jackson.JsonMethods._
import rep.proto.rc2.ActionResult
import rep.sc.scalax.{ContractContext, IContract}

/**
 *
 * @param height        被审查的区块高度
 * @param hash_tx       被审查区块的选择性隐藏 Hash，用于验证内容
 * @param illegal_txIds 违规交易Map, key:tixd, value:违规原因。
 */
final case class Conclusion(height: Long, hash_tx: String, illegal_txIds: Map[String, String])

class RegulateTPL extends IContract {
  def init(ctx: ContractContext): Unit = {
    println(s"tid: ${ctx.t.id}, contract: ${ctx.t.getCid.chaincodeName}, version: ${ctx.t.getCid.version}")
  }

  /**
   * @description: 交易监管, 把区块也做一个标记，这样就可以先判断区块中是否有违规的交易，如果有找出违规交易，否则不再遍历交易
   * @author: daiyongbing
   * @param conslusions 监管结论数组
   * @date: 2023/3/16
   * @version: 1.0
   */
  def regBlocks(ctx: ContractContext, conslusions: Seq[Conclusion]): ActionResult = {
    conslusions.foreach(c => {
      val preKey = c.hash_tx + "_" + c.height + "_"
      for ((txid, code) <- c.illegal_txIds) {
        ctx.api.setVal(preKey + txid, code)
      }
      ctx.api.setVal(c.hash_tx + "_" + c.height, true)
    })
    null
  }

  // 用作测试，实际部署时去掉该方法
  def test(ctx: ContractContext, map: Map[String, String]): ActionResult = {
    for ((k, v) <- map) {
      ctx.api.setVal(k, v)
    }
    null
  }

  def onAction(ctx: ContractContext, action: String, sdata: String): ActionResult = {

    implicit val formats = DefaultFormats
    val json = parse(sdata)

    action match {
      case "regBlocks" =>
        regBlocks(ctx, json.extract[Seq[Conclusion]])

      case "test" =>
        test(ctx, json.extract[Map[String, String]])
    }
  }
}