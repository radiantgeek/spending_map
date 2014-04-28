package services

import scala.concurrent._

import play.api._
import play.api.libs.json._
import play.api.libs.ws._

import scala.util.Random
import java.net.URLEncoder

import models._
import lib.ws.WsService.RequestConfigurer

// ------------------------------------------------------------------------------------------------------

case class GeoResponse(status: Int, text: String,
                       lat: Double, lon: Double,
                       city: String, streetType: String, streetName: String, house: String,
                       debug: String="") {
  override def toString() = s"($lat, $lon)[st=$status]->'$text'"
}

object GeoResponse {
  implicit val format = Json.format[GeoResponse]
}

// ------------------------------------------------------------------------------------------------------

object GeoCoder extends GeoService with OsmGeoService {

  lazy val log        = Logger("services.geocoder")

  val elasticService  = Elastic

  val ya_url          = "http://geocode-maps.yandex.ru/1.x/?format=json&geocode="
  val goo_url         = "http://maps.googleapis.com/maps/api/geocode/json?sensor=false&language=ru&address="

  def request(address: String): Future[GeoResponse] = {
    implicit val ctx = Contexts.geocoderCtx
    _osm_request(address)
  }

  def requestAsJson(address: String): Future[JsValue] = {
    implicit val ctx = Contexts.geocoderCtx
    request(address).map { res =>
      Json.obj(
        ElasticTypes.Geocoder._name -> Json.obj(
          "data" -> Json.arr(Json.obj(
            "address" -> address,
            "geo" -> res
          ))
        )
      )
    }
  }
}

// ------------------------------------------------------------------------------------------------------
trait OsmGeoService extends GeoService {

  val elasticService: ElasticSearch

  def _osm_params(address: String): Seq[(String, String)] =
    catchException("osm address="+address, Seq("+strType" -> "1")) {
      val s = address.
        replaceAll("Российская Федерация\\s*,\\s*", "").
        replaceAll("(г\\.|город)?\\s*Москва\\s*,\\s*", "").
        replaceAll("\\s*,?\\s*-\\s*\\.?\\s*", "").
        replaceAll("\\d{6,6}\\s*,\\s*", "").
        trim.split("\\s*,\\s*")
      log.debug(s"try found address: $address, trimmed: "+s.mkString("|"))

      val (strType, strName)  = OsmLoader.parseStreet(s(0))
      val home                = OsmLoader.filterHome(s(1))

      var params = Seq("+streetName" -> strName)
      if (home.nonEmpty)    { params = params ++ Seq("+home" -> home.replaceAll("/", " ")) }
      //      if (home.nonEmpty)    { params = params ++ Seq("+home" -> elasticService.encode(home)) }
      //      if (home.nonEmpty)    { params = params ++ Seq("+home" -> home.replaceAll("/", "\\\\/")) }
      //      if (home.nonEmpty)    { params = params ++ Seq("+home" -> elasticService.encode(home.replaceAll("/", "\\\\/"))) }
      if (strType.nonEmpty) { params = params ++ Seq("+streetType" -> strType) }
      params
    }

  def _osm_request(address: String): Future[GeoResponse] = {
    val params = _osm_params(address)
    val queryParams: RequestConfigurer = { f => f.withQueryString("size" -> "3") }

    elasticService._searchLite(ElasticTypes.OsmCoder, params:_*)(queryParams).map { js =>
      catchException("osm address="+address) {
        val found = (js \ "hits" \ "hits" \\ "_source").toList
        val a = found.head
        GeoResponse(found.size, address,
          (a \ "lat").as[Double], (a \ "lon").as[Double],
          "Москва", (a \ "streetType").as[String], (a \ "streetName").as[String], (a \ "home").as[String])
      }
    }(Contexts.osmGeocoderCtx)
  }
}


// ------------------------------------------------------------------------------------------------------

trait GeoService {
  val log: Logger

  def request(address: String): Future[GeoResponse]
  def requestAsJson(address: String): Future[JsValue]

  def request(url: String, address: String)(worker: JsValue => GeoResponse)(implicit executor: ExecutionContext): Future[GeoResponse] = {
    val start = System.currentTimeMillis()

    val link = url + URLEncoder.encode(address, "utf-8")
    log.debug(link)

    Thread.sleep(100+Random.nextInt(200))
    WS.url(link).get().map {
      response => {
        val t2 = System.currentTimeMillis() - start
        val log = t2 + " ms: <a href='"+link+"' target='_blank'>check@coder</a>  <i>'" + address+ "'</i>"

        catchException(log) {
          Thread.sleep(3000+Random.nextInt(300))
          val res = worker(response.json)
          res.copy(debug = (log + " <br/> <small>" + res.debug + "</small> <br/> \n"))
        }
      }
    }

  }

  def catchException(debug: String)(worker: => GeoResponse): GeoResponse =
    catchException(debug, GeoResponse(-127, "", 0, 0, "", "", "", ""))(worker)

  def catchException[T](debug: String, default: T)(worker: => T): T=
    try {
      worker
    } catch { case e: Throwable =>
        log.debug(s"Exception: ($debug) ${e.getLocalizedMessage} ")
        default
    }

}

