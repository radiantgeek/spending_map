package models

import play.api.libs.json._
import org.joda.time._

import lib.DateUtils

/**
 */
object ContractInfo {

  val isoFormat = DateUtils.isoFormat

  // ----------------------------------------------------------------------

  case class Info(contract: JsValue, customer: JsValue, supplier: JsValue, custGeo: JsValue, suppGeo: JsValue)

  // ----------------------------------------------------------------------

  case class AddrView(status: JsValue, lat: JsValue, lon: JsValue)
  case class InfoView(
                       // contract
                       cRegNum: JsValue,
                       cSignDate: JsValue, cExecDate: JsValue, cDateDiff: JsValue,
                       cPrice: JsValue,
                       ctRegNum: JsValue, ctName: JsValue, ctAddr: AddrView,                 // customer
                       spInn: JsValue,   spKpp: JsValue, spName: JsValue, spAddr: AddrView   // supplier
                       )

  // ----------------------------------------------------------------------

  object AddrView {
    implicit val format = Json.format[AddrView]
  }
  object InfoView {
    implicit val format = Json.format[InfoView]
  }

  // ----------------------------------------------------------------------

  def infoToView(info: Info) = info match {

    /**
     * [!] c \ "finances" \ "budgetLevel" \ "code"
     * [!] c \ "products" \ "product" \ "quantity"
     * [!] c \ "products" \ "product" \ "OKDP" \ "name"
     * [!] c \ "products" \ "product" \ "OKEI" \ "name"
     * [-] c \ "finances" \ "budget" \ "name"
     * [-] c \ "finances" \ "budgetary" \\ ["month", "year", "price"]
     * [-] c \ "protocolDate", c \ "publishDate", c \ "execution" \ ["month", "year"] [difference with signDate?]
     * [-] ct \ "accounts" \ "account" \ "bankName"
     * [-] ct \ "accounts" \ "account" \ "bankName"
     * [-] ct \ "contractsCount"
     * [-] ct \ "contractsSum"
     * [-] ct \ "organizationType" \ "name"
     * [-] ct \ "subordinationType"
     * [-] ct \ "okopf" \ "fullName"
     * [-] ct \ "okogu" \ "name"
     * [!] sp \ "organizationForm"
     * [?] sp \ "contractsCount"
     * [?] sp \ "contractsSum"
     * [-] sp \ "regionCode"
     *
     */
    case Info(c, ct, sp: JsValue, ctGeo: JsValue, spGeo: JsValue) =>
      val year  =  (c \ "execution" \ "year").as[String].toInt
      val month =  (c \ "execution" \ "month").as[String].toInt
      val signDate = isoFormat.parseDateTime( (c \ "signDate").as[String] )
      val execDate = new DateTime(year, month, 1, 0, 0)
      val sExecDate = JsString(isoFormat.print(execDate))
      val dateDiff = Days.daysBetween(signDate.withTimeAtStartOfDay(), execDate.withTimeAtStartOfDay() ).getDays()
      InfoView(
        c \ "regNum",
        c \ "signDate", sExecDate, JsNumber(dateDiff),
        c \ "price",
        ct \ "regNumber", ct \ "fullName",               geotoView(ctGeo),
        sp \ "inn", sp \ "kpp", sp \ "organizationName", geotoView(spGeo)
      )
  }

  def geotoView(geo: JsValue) = {
    val addr = (geo \ "geocoder" \ "data" \\ "geo").head
    AddrView( addr \ "status", addr \ "lat", addr \ "lon" )
  }
}
