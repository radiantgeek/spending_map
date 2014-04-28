package services

import scala.concurrent._

import play.api._
import play.api.libs.json._
import lib.ws.WsService.RequestConfigurer

import models._

/**
 *
 */
trait CachedService {

  implicit val ctx: ExecutionContext
  val writeCtx: ExecutionContext
  val log: Logger

  def NOT_FOUND(typ: EType) = {
    Json.obj(typ._name -> Json.obj("data" -> Json.arr("Data not found")))
  }
  def NOT_FOUND_GEO(typ: EType) = {
    Json.obj(typ._name -> Json.obj("data" -> Json.arr(
      Json.obj(
        "address" -> "",
        "geo" -> Json.obj("status" -> -1, "lat" -> 0, "lon" -> 0
        ))
    )))
  }

  val elasticService:   ElasticSearch
  val spendingService:  SpendingService
  val geoService:       GeoService

  // ----------------------------------------------------------------------
  def osmGeoAddress(address: String)(result: => JsValue): Future[JsValue] = {
    cachedSearch(ElasticTypes.OsmCoder, "+address" -> ("\""+address+"\"")) {
      Future { result } (writeCtx)
    }
  }

  // ----------------------------------------------------------------------

  def geocode(address: String): Future[JsValue] = {
    _checkEmpty(ElasticTypes.Geocoder, address)(NOT_FOUND_GEO) {
      cachedSearch(ElasticTypes.Geocoder, "+address" -> ("\"" + address + "\"")) {
        geoService.requestAsJson(address)
      }
    }
  }

  // ----------------------------------------------------------------------

  def local_contracts(page: Long, params: (String, String)*): Future[JsValue] = {
    val pp = ParametersMapping.paramToElastic(params :_*)
    log.debug(s"retrieve local contracts for ${pp}")

    val limit = 50
    val off   = (page-1)*limit
    val queryParams: RequestConfigurer = { f => f.withQueryString("size" -> limit.toString, "from" -> off.toString) }

    elasticService._searchLite(ElasticTypes.Contracts, pp: _*)(queryParams).map { js =>
      val list = (js \ "hits" \ "hits" \\ "_source")
      Json.obj(
        "contracts" -> Json.obj(
          "total"   -> (js \ "hits" \ "total"),
          "page"    -> JsNumber(page),
          "perpage" -> JsNumber(limit),
          "current" -> list.seq.size,
          "data"    -> JsArray(list)
        )
      )
    }
  }

  // ----------------------------------------------------------------------

  def remote_contracts(page: Long, params: (String, String)*): Future[JsValue] = {
    val pp = params ++ Seq(
      "page"          -> page.toString,
//      "returnfields"  -> "[regNum,customer.regNum,suppliers.supplier.inn,suppliers.supplier.kpp]"
      "returnfields"  -> "[regNum,signDate]"
    )
    spendingService.contractsSelect(pp: _*)
  }
  // ----------------------------------------------------------------------


  def contract(regNum: String): Future[JsValue] = {
    _checkEmpty(ElasticTypes.Contracts, regNum)(NOT_FOUND) {
      cachedSearch(ElasticTypes.Contracts, "regNum" -> regNum) { // heavy ES search :(
        spendingService.contract(regNum)
      }
    }
  }

  def contract(obj: JsValue): Future[JsValue] = {
    cachedSearch(ElasticTypes.Contracts, obj) {                 // light ES 'get' request
      spendingService.contract(ElasticTypes.Contracts._id(obj))
    }
  }

  def customer(regNum: String): Future[JsValue] = {
    _checkEmpty(ElasticTypes.Customers, regNum)(NOT_FOUND) {
      cachedSearch(ElasticTypes.Customers, regNum) {
          spendingService.customer(regNum)
      }
    }
  }

  def supplier(inn: String, kpp: String): Future[JsValue] = {
    _checkEmpty(ElasticTypes.Suppliers, inn, kpp)(NOT_FOUND) {
      cachedSearch(ElasticTypes.Suppliers, inn + "_" + kpp) {
          spendingService.supplier(inn, kpp)
      }
    }
  }

  // ----------------------------------------------------------------------

  def cachedSearch(typ: EType, params: (String, String)*)(remote: => Future[JsValue]): Future[JsValue] = {
    _cachedSearch(typ)(remote) {
      elasticService._searchLite(typ, params: _*).map { js =>
        (js \ "hits" \ "hits" \\ "_source").head
      }
    }
  }

  def cachedSearch(typ: EType, id: String)(remote: => Future[JsValue]): Future[JsValue] = {
    _cachedSearch(typ)(remote){
      elasticService._get(typ, id).map { js =>
        (js \\ "_source").head
      }
    }
  }

  def cachedSearch(typ: EType, obj: JsValue)(remote: => Future[JsValue]): Future[JsValue] = {
    _cachedSearch(typ)(remote){
      elasticService._get(typ, obj).map { js =>
        (js \\ "_source").head
      }
    }
  }

  def _cachedSearch(typ: EType)(remote: => Future[JsValue])(local: => Future[JsValue]): Future[JsValue] = {
    val f = _recover(local) {
      remote.map(_cached(typ) _)
    }
    f onFailure {
      case e: Throwable => log.error("error in remote part", e)
    }
    f
  }

  // ----------------------------------------------------------------------
  //  utils

  def _recover[T](first: Future[T])(second: => Future[T]): Future[T] = {
    first recoverWith { case e: Any =>
      second
    }
  }

  def _cached(typ: EType)(found: JsValue): JsValue = {
    val id = elasticService.encode(typ._id(found))
    if (id.nonEmpty) {
      elasticService._put(typ, id, found) // .map { res => println(res) }
    } else
      log.error(s"id was empty for ${typ._name}. json="+found)
    found
  }


  def _checkEmpty(typ: EType, params: String*)(notFound: EType=>JsValue)(worker: => Future[JsValue]): Future[JsValue] = {
    val isEmpty = params.filter(_.isEmpty).size > 0 // at least one parameter is empty
    if (isEmpty) {
      Future {
        notFound(typ)
      }
    } else {
      worker
    }

  }

}
