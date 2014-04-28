package controllers

import services._
import models._
import ContractInfo._

import play.api._
import play.api.libs.json._

import scala.concurrent._
import lib.Conf

// ------------------------------------------------------------------------------------------------------

/**
 *
 */
object ServicePoint extends CachedService with Conf {

  implicit val ctx    = Contexts.cacherCtx
  val writeCtx        = Contexts.cacherWriteCtx

  lazy val log        = Logger("services.cacher")

  var localMode       = conf.getBoolean("spending.localmode").getOrElse(false)

  val elasticService  = Elastic
  val spendingService = ClearSpending
  val geoService      = GeoCoder

  // ----------------------------------------------------------------------

  def requestToParameters(req: SearchRequest) = {
    var params = Seq(
      "daterange"       -> s"${req.dateFrom}-${req.dateTo}",
      "customerregion"  -> "77"
    )
    if (req.priceFrom>0 || req.priceTo>0) {
      params = params ++ Seq("pricerange"     -> s"${req.priceFrom}-${req.priceTo}")
    }
    if (req.text.nonEmpty) {
      params = params ++ Seq("productsearch"  -> req.text.get) // ?
    }
    params
  }

  def summarize(page: Long, req: SearchRequest): Future[JsValue] = {
    val params = requestToParameters(req)
    val p      = page
    log.debug(s"retrieve $page page contracts for ${params}")

    val res = for {
      js        <- contracts(p, params :_*)
      list      <- retrieveInfoList(js, req)
    } yield {
      val filtered = list.filter(filterResults(req)).map { i => Json.toJson(infoToView(i)) }
      Json.obj(
        "total"     -> js \ "contracts" \ "total",
        "page"      -> js \ "contracts" \ "page",
        "perpage"   -> js \ "contracts" \ "perpage",
        "sended"    -> list.size,
        "filtered"  -> filtered.size,
        "data"      -> filtered
      )
    }
    res onFailure {
      case e: Throwable => {
        log.error(e.getMessage)
      }
    }
    res
  }

  def filterResults(req: SearchRequest)(i: Info): Boolean = {
    def checkPoint(p: JsValue) = {
      val lat = (p\"lat").as[Double]
      val lon = (p\"lon").as[Double]
      ((p \ "status").as[Int]>0) && (req.sw_lat <= lat) && (lat <= req.ne_lat) && (req.sw_lng <= lon) && (lon <= req.ne_lng)
    }

    // 1. check req - geometry
    var a = true
    if (req.geo) {
      a = a && {
        val a: Seq[JsValue] = (i.custGeo \ "geocoder" \ "data" \\ "geo") ++ (i.suppGeo \ "geocoder" \ "data" \\ "geo")
        a.filter(checkPoint).nonEmpty
      }
    }

    // 2. check req - budget level
    a = a && {
      try {
        val level = (i.contract \ "finances" \ "budgetLevel" \ "code").as[String]
        req.budget.contains(level.toInt)
      } catch {
        case e: Throwable => true
      }
    }
    a
  }

  def contracts(page: Long, params: (String, String)*): Future[JsValue] = {
    if (localMode) {
      local_contracts(page, params: _*)
    } else {
      remote_contracts(page, params :_*)
    }

  }

  // ----------------------------------------------------------------------

  def save_search(req: SearchRequest): Future[JsValue] = {
    elasticService._post(ElasticTypes.SearchHistory, Json.toJson(req))
  }

  def find_search(_id: String): Future[SearchRequest] = {
    elasticService._get(ElasticTypes.SearchHistory, _id).map { js =>
      (js \ "_source").as[SearchRequest].copy(id=Some(_id))
    }
  }
  def update_search(_id: String, title: String): Future[SearchRequest] = {
    val data = Json.obj("title" -> title, "published" -> true, "approved" -> false)
    elasticService._part_update(ElasticTypes.SearchHistory, _id, data).map { js =>
      (js \ "_source").as[SearchRequest].copy(id=Some(_id))
    }
  }
  def approve_search(_id: String, title: String): Future[SearchRequest] = {
    val data = Json.obj("title" -> title, "approved" -> true)
    elasticService._part_update(ElasticTypes.SearchHistory, _id, data).map { js =>
      (js \ "_source").as[SearchRequest].copy(id=Some(_id))
    }
  }
  // ----------------------------------------------------------------------

  // retrieve full info about contract(+supplier, +customer) by short search result
  def retrieveInfo(jc: JsValue): Future[Option[Info]] = {
    log.debug("retrieve contract " + jc)
    spendingService._recoverFutureException("retrieve info about contract #"+jc) {
      for {
        c <- contract(jc)
        ct <- customer(_s(c \ "customer" \ "regNum"))
        s <- supplier(_s(c \ "suppliers" \ "supplier" \ "inn"), _s(c \ "suppliers" \ "supplier" \ "kpp"))
        addr <- getAddresses(c, ct, s)
      } yield {
        Info(c, ct, s, addr._1, addr._2)
      }
    }
  }

  def retrieveInfoList(js: JsValue, req: SearchRequest): Future[Seq[Info]] = {
    log.debug("retrieve info list, total " + (js \ "contracts" \ "total"))

    spendingService._catchException(Json.prettyPrint(js), "contracts/list") { // TODO: very expensive logging :(
      val jsList = (js \ "contracts" \ "data").as[JsArray].value
      val res = Future.sequence( jsList.map(retrieveInfo) )
      // skip empty elements (exception)
      _filterNone( res )
    }
  }
  def _filterNone(list: Future[Seq[Option[Info]]]) = list.map { list =>
    list.filter(_.nonEmpty).map(_.get) // skip None from list
  }
  def _filter(list: Future[Seq[Info]])(f: Info => Boolean) = list.map { list =>
    list.filter(f)
  }

  // ----------------------------------------------------------------------
  // histo

  def _histo: Future[JsValue] = {
    elasticService._search(ElasticTypes.Contracts, _histo_query).map { js =>
      (js \ "facets" \ "0" \ "entries")
//      (js \ "facets" \ "0" \ "entries").as[JsArray].value
    }
  }

  def _histo_query: JsValue = {
    val t1 = SearchRequest.getDate("01.01.2011").getMillis.toString
    val t2 = SearchRequest.getDate("01.01.2014").getMillis.toString
    Json.obj("size" -> 0, "facets" -> Json.obj("0" -> Json.obj(
      "global" -> true,
      "date_histogram" -> Json.obj("field" -> "signDate", "interval" -> "1M"),
      "facet_filter" -> Json.obj("fquery" -> Json.obj("query" -> Json.obj("filtered" -> Json.obj(
        "query" -> Json.obj("query_string" -> Json.obj("query" -> "*")),
        "filter" -> Json.obj("bool" -> Json.obj("must" -> Json.obj("range" ->
          Json.obj("signDate" -> Json.obj("from" -> t1, "to" -> t2))
        )))
      ))))
    )))
  }

  // ----------------------------------------------------------------------

  def getAddresses(c: JsValue, ct: JsValue, s: JsValue): Future[(JsValue, JsValue)] = {
    log.debug("retrieve address for contract " + (c \ "regNum"))
    for {
      custGeo <- geocode(_s(ct \ "factualAddress" \ "addressLine"))
      suppGeo <- geocode(_s(s  \ "factualAddress"))
    } yield {
      (custGeo, suppGeo)
    }
  }

  // ----------------------------------------------------------------------

  def _s(js : JsValue) = js.asOpt[String].getOrElse("").trim

}

// ------------------------------------------------------------------------------------------------------
