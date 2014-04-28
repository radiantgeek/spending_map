package models

import play.api.libs.json._

import org.joda.time._
import lib.DateUtils

// ----------------------------------------------------------------------

trait EType {
  def _type: String
  def _name: String

  // index for searching
  def _index: String
  // index for saving
  def _index(js: JsValue): String
  // id for saving
  def _id(js: JsValue): String = _s(js \ "_id")

  // utils
  def _s(js : JsValue) = js.asOpt[String].getOrElse("")
}

class ETypeSimple(name: String) extends EType {
  def _index              = name
  def _index(js: JsValue) = name
  def _type               = name
  def _name               = name
}

class ETypeSharded(name: String) extends EType {
  def _index              = "_all"
  def _index(js: JsValue) = name
  def _type               = name
  def _name               = name
}

// ----------------------------------------------------------------------

/**
 */
object Spending {
  val Contracts = new ETypeSharded("contracts") {
    override def _index               = "spending"
    override def _index(js: JsValue)  = "spending"
    override def _id(js: JsValue)     = _s(js \ "regNum")
  }

  val Customers = new ETypeSimple("customers") {
    override def _index               = "spending"
    override def _index(js: JsValue)  = "spending"
    override def _id(js: JsValue)     = _s(js \ "regNumber")
  }

  val Suppliers = new ETypeSimple("suppliers") {
    override def _index               = "spending"
    override def _index(js: JsValue)  = "spending"
    override def _id(js: JsValue)     = _s(js \ "inn") + "_" + _s(js \ "kpp")
  }
}

object ElasticTypes {

  val SearchHistory = new ETypeSimple("history") {
  }

  val Contracts = new ETypeSharded("contracts") {
    override def _id(js: JsValue)    = _s(js \ "regNum")
    override def _index              = _name + "*"
    override def _index(js: JsValue) = {
      _name + "_" + _s(js \ "signDate").substring(0, 7).replaceAll("-", "")
    }
  }

  val Customers = new ETypeSimple("customers") {
    override def _id(js: JsValue) = _s(js \ "regNumber")
  }

  val Suppliers = new ETypeSimple("suppliers") {
    override def _id(js: JsValue) = _s(js \ "inn")+"_"+_s(js \ "kpp")
  }

  val Geocoder  = new ETypeSimple("geocoder") {
    override def _id(js: JsValue) = _s((js \ _name \ "data" \\ "address").head)
  }

  val OsmCoder  = new ETypeSimple("osmcoder")  {
    override def _id(js: JsValue) = _s(js \ "address")
  }

  val Logstash  = new ETypeSharded("playlog") {
    val indexFmt = DateUtils.indexFmt

    override def _index(js: JsValue) = {
      val now = new DateTime(DateTimeZone.UTC)
      "logstash-" + indexFmt.print(now)
    }
  }

}
