/**
 * Copyright  2024 Linkel Technology Co., Ltd, Beijing.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BA SIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rep.sc.tpl

import org.json4s
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization.write
import org.slf4j.Logger
import rep.proto.rc2.ActionResult
import rep.sc.scalax.{ContractContext, ContractException, IContract}


case class LedgerProofData(uuid: String, data: String)

/**
 * Created by 北京连琪科技有限公司.
 */
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
  private def proofLedgerData(ctx: ContractContext, data: String) = {
    val ledgerDataList = parse(data).extract[List[LedgerProofData]]
    ledgerDataList.foreach(ledgerData => {
      ctx.api.setVal(ledgerData.uuid, ledgerData.data)
    })
    ActionResult()
  }

  def onAction(ctx: ContractContext, action: String, sdata: String): ActionResult = action match {
    case "proofLedgerData" => proofLedgerData(ctx, sdata)
    case _ => throw ContractException("该合约没有改方法")
  }

}
