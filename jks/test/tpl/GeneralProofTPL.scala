package rep.sc.tpl

import org.json4s
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization.write
import org.slf4j.Logger
import rep.proto.rc2.ActionResult
import rep.sc.scalax.{ContractContext, ContractException, IContract}


case class LedgerProofData(uuid: String, data: String)

class GeneralProofTPL extends IContract {

  implicit val formats: json4s.DefaultFormats.type = DefaultFormats
  var logger: Logger = _

  def init(ctx: ContractContext): Unit = {
    this.logger = ctx.api.getLogger
    logger.info(s"init | 初始化通用存证合约：${ctx.t.cid.get.chaincodeName}, 交易ID为：${ctx.t.id}")
  }

  /**
   * 存证数据
   *
   * @param ctx
   * @param data
   * @return
   */
  private def proofLedgerData(ctx: ContractContext, data: String): ActionResult = {
    val ledgerDataList = parse(data).extract[List[LedgerProofData]]
    ledgerDataList.foreach(ledgerData => {
      ctx.api.setVal(ledgerData.uuid, ledgerData.data)
    })
    ActionResult()
  }

  def onAction(ctx: ContractContext, action: String, sdata: String): ActionResult = {
    action match {
      case "proofLedgerData" => proofLedgerData(ctx, sdata)
      case _ => throw ContractException("该合约没有改方法")
    }
  }

}
