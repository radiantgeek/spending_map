package models

import org.joda.time.DateTime

import play.api.libs.json.Json
import play.api.data.Form
import play.api.data.Forms._
import lib.DateUtils

case class PublishRequest(id: String, text: String)

case class SearchRequest(
                          id:     Option[String],
                          text:   Option[String],
                          date_from:  DateTime,
                          date_to:    DateTime,
                          price_from: Long,
                          price_to:   Long,

                          budget: Seq[Int],

                          geo:    Boolean,
                          sw_lat: BigDecimal,
                          sw_lng: BigDecimal,
                          ne_lat: BigDecimal,
                          ne_lng: BigDecimal,

                          published:  Option[Boolean],
                          approved:   Option[Boolean],
                          title:      Option[String]
) {

  def dateFrom  = date_from.toString(SearchRequest.dateFormat)
  def dateTo    = date_to.toString(SearchRequest.dateFormat)
  def priceFrom = price_from
  def priceTo   = price_to

}

object SearchRequest {
  implicit val format = Json.format[SearchRequest]

  val dateFormat = DateUtils.rusFormat

  def getDate(s: String) = DateTime.parse(s, dateFormat)

  def getBudget(sForm: Form[SearchRequest], id: Int): Boolean = sForm.get.budget.contains(id)

  def DEFAULT = {
    val startDate = SearchRequest.getDate("01.01.2012")
    val stopDate = SearchRequest.getDate("01.02.2012")
    SearchRequest(None,
      None, startDate, stopDate, 0, 0, Seq(1, 2), false, 0, 0, 0, 0,
      None, None, None)
  }

  def _form = Form(
    mapping(
      "id" -> optional(text),
      "phrase" -> optional(text),
      "date_from" -> jodaDate("dd.MM.YYYY"),
      "date_to" -> jodaDate("dd.MM.YYYY"),
      "price_from" -> longNumber,
      "price_to" -> longNumber,

      "budget" -> seq(number),

      "geo" -> boolean, // TODO
      "sw_lat" -> bigDecimal,
      "sw_lng" -> bigDecimal,
      "ne_lat" -> bigDecimal,
      "ne_lng" -> bigDecimal,

      "published" -> optional(boolean),
      "approved" -> optional(boolean),
      "title" -> optional(text)
    )(SearchRequest.apply)(SearchRequest.unapply)
  )
}

object PublishRequest {
  def _publish_form = Form(
    mapping(
      "id"          -> text,
      "text"        -> text
    )(PublishRequest.apply)(PublishRequest.unapply)
  )
}
