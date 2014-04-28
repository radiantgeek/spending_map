package models

import anorm._
import play.api.libs.json._

trait PkFormatter {
  implicit object PkFormat extends Format[Pk[Long]] {
    def reads(json: JsValue):JsResult[Pk[Long]] = JsSuccess(Id(json.as[Long]))
    def writes(id: Pk[Long]):JsNumber = JsNumber(id.get)
  }
}
