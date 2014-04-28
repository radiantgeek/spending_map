package services

import lib._
import lib.ws._
import lib.ws.WsService._

import play.api._
import play.api.libs.json._

import scala.concurrent._

import models._

// ----------------------------------------------------------------------

object Elastic extends ElasticSearch with Conf {
  implicit val ctx  = Contexts.elasticCtx
  implicit val auth: Auth = Auth.no

  lazy val server = conf.getString("elastic.server").get
  lazy val log    = Logger("services.elastic")

  // ----------------------------------------------------------------------

  def _cmd(cmd: String) = _request(s"$server/$cmd")

}

// ----------------------------------------------------------------------

trait ElasticSearch extends WsService {
  implicit val auth: Auth
  implicit val ctx: ExecutionContext

  // ----------------------------------------------------------------------

  def _search(typ: EType, request: JsValue, searchType: String="count"): Future[JsValue] = timedJson {
    val idx = typ._index
    val tp  = typ._type
    log.debug { (s"POST /[$idx]/[$tp]/_search?search_type=$searchType") }
    _cmd(s"$idx/$tp/_search?search_type=$searchType").post[JsValue](request)
  }
  // ----------------------------------------------------------------------

  def _post(typ: EType, data: JsValue): Future[JsValue] = timedJson {
    val idx = typ._index(data)
    val tp  = typ._type
    log.debug { (s"POST /[$idx]/[$tp]") }
    _cmd(s"$idx/$tp").post[JsValue](data)
  }

  def _put(typ: EType, id: String, data: JsValue): Future[JsValue] = timedJson {
    val idx = typ._index(data)
    val tp  = typ._type
    log.debug { (s"PUT /[$idx]/[$tp]/[$id]") }
    _cmd(s"$idx/$tp/$id").put[JsValue](data)
  }

  // partial update:
  //   http://www.elasticsearch.org/guide/en/elasticsearch/guide/current/partial-updates.html
  def _part_update(typ: EType, id: String, data: JsValue): Future[JsValue] = timedJson {
    val idx = typ._index(data)
    val tp  = typ._type
    log.debug { (s"POST /[$idx]/[$tp]/[$id]") }
    val change = Json.obj("doc" -> data)
    _cmd(s"$idx/$tp/$id/_update").post[JsValue](change)
  }

  def _get(typ: EType, id: String): Future[JsValue] = timedJson {
    val idx = typ._index
    val tp  = typ._type
    log.debug { (s"GET /[$idx]/[$tp]/[$id]") }
    _cmd(s"$idx/$tp/$id").get()
  }

  def _get(typ: EType, obj: JsValue): Future[JsValue] = timedJson {
    val idx = typ._index(obj)
    val tp  = typ._type
    val id  = typ._id(obj)
    log.debug { (s"GET /[$idx]/[$tp]/[$id] {$obj}") }
    _cmd(s"$idx/$tp/$id").get()
  }

  // multi-get:
  //  http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/docs-multi-get.html
  def _get(typ: EType, id: String*): Future[JsValue] = timedJson {
    val idx = typ._index
    val tp  = typ._type
    val list = Json.obj("docs" -> Json.arr(id))
    log.debug { (s"GET /[$idx]/[$tp]/[$id]") }
    _cmd(s"$idx/$tp/_mget").post[JsValue](list)
  }

  // ----------------------------------------------------------------------

  def _searchLite(typ: EType, params: (String, String)*)
                 (implicit requestConfigurer: RequestConfigurer): Future[JsValue] = timedJson {
    val idx = typ._index
    val tp  = typ._type
    val q   = _params(params :_* )

    log.debug { (s"search in [$idx][$tp] for -> $q") }
    var r = _cmd(s"$idx/$tp/_search").withQueryString("q" -> q)
    r = requestConfigurer(r)
//    println(r.url+" : "+r.queryString)
    r.get()
  }

  def _count(typ: EType, params: (String, String)*): Future[Long] = timedJson {
    val idx = typ._index
    val tp  = typ._type
    val q   = _params(params :_* )

    log.debug { (s"count in [$idx][$tp] for -> $q") }
    val r = _cmd(s"$idx/$tp/_count").withQueryString("q" -> q)
    r.get()
  }.map { js => (js \ "count").as[Long] }

  // ----------------------------------------------------------------------

  def _params(params: (String, String)*) = {
    // params.map { i => i._1+":"+i._2 }.mkString(" AND ")
    // params.map { i => i._1+":"+encode(i._2) }.mkString(" ")
    params.map { i => i._1+":"+i._2 }.mkString(" ")
  }

  //  not tested
  def _mapping(typ: EType, data: JsValue): Future[JsValue] = timedJson {
    val idx = typ._index
    val tp  = typ._type
    log.debug { (s"PUT /[$idx}]/[$tp]/_mapping") }
    _cmd(s"$idx/$tp/_mapping").put[JsValue](data)
  }

}
