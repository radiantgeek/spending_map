package services

import lib._
import models._

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.ws.WS

import org.joda.time._
import org.joda.time.format._
import scala.concurrent.ExecutionContext
import play.api.libs.json.Json.JsValueWrapper

/**
 * Save log to LogStash
 *
 * Based on example from Matthias Nehlsen:
 *    http://matthiasnehlsen.com/blog/2013/07/09/transforming-logs-into-information/
 *    https://github.com/matthiasn/sse-chat/blob/130707-kibana-demo/app/utilities/Logger.scala
 *
 * TODO: Think about installling local geoip service ( https://github.com/fiorix/freegeoip )
 */

object LogPoint extends LogService {
  val elasticService  = Elastic
  val geoIpCtx        = Contexts.geoipCtx
}

// ----------------------------------------------------------------------

trait LogService extends Conf {
  val elasticService: ElasticSearch
  val geoIpCtx: ExecutionContext

  val instanceID  = conf.getString("application.instanceID").get
  val geoIpUrl    = conf.getString("geo_ip.url").get

  val stampFormat = DateUtils.stampFormat
  val indexFmt    = DateUtils.indexFmt

  val TRACE = "TRACE"
  val DEBUG = "DEBUG"
  val INFO  = "INFO"
  val ERROR = "ERROR"

  // ----------------------------------------------------------------------

  /** LogStash-format logger, allows passing anything that can be expressed as a JsValue in addition to standard fields
    * @param sourcePath  source path of event
    * @param msg         event message
    * @param eventType   event type
    * @param fields      arbitrary data as JsValue
    **/
  def log(sourcePath: String, msg: String, eventType: String, fields: Option[JsValue]=None, tags: JsArray=JsArray()) {

    val now = new DateTime(DateTimeZone.UTC)
    val logItem = Json.obj(
      "@source"       -> instanceID,
      "@tags"         -> tags,
      "@fields"       -> fields,
      "@timestamp"    -> stampFormat.print(now),
      "@source_host"  -> "mn.local",
      "@source_path"  -> sourcePath,
      "@message"      -> msg,
      "@type"         -> eventType
    )

    elasticService._post(ElasticTypes.Logstash, logItem)
  }

  // ----------------------------------------------------------------------

  /** Simple request logger, logs request including Geo Data if available. Uses LogStash-style logger above.
    * @param req       request
    * @param msg       event message
    * @param httpCode  HTTP code of connection (e.g. 200 or 404)
    * @param duration  duration of connection in milliseconds
    **/
  def logRequest(req: Request[_], msg: String, httpCode: Int, duration: Long,
                 fields: (String, JsValueWrapper)*) {
    val userAgent = req.headers.get("User-Agent").getOrElse("")

    // basic request log item
    val logItem = Json.obj(
      "instanceID"  -> instanceID,
      "request"     -> req.toString(),
      "ip"          -> req.remoteAddress,
      "requestID"   -> req.id,
      "user-agent"  -> userAgent,
      "httpCode"    -> httpCode,
      "duration_ms" -> duration
    ) ++ Json.obj( fields :_* )

    // freegeoip needs IPv4 addresses, ignore local requests with IPv6 addresses for logging
    if (req.remoteAddress.contains(":")) {
      log(req.toString(), msg, INFO, Some(logItem))
    } else {
      val geoRequest = WS.url(geoIpUrl + req.remoteAddress).
        withRequestTimeout(2000).
        get().map { case res => { // log with geo data if service accessible
          val point = "["+(res.json \ "longitude").as[Double]+","+(res.json \ "latitude").as[Double]+"]"
          val geoLogItem = logItem ++ Json.obj(
              "country_code"  -> res.json \ "country_code",
              "country"       -> res.json \ "country_name",
              "region_code"   -> res.json \ "region_code",
              "region"        -> res.json \ "region_name",
              "geo"           -> point
          )
          log(req.toString(), msg, INFO, Some(geoLogItem))
        } }(geoIpCtx)

      // log without geo data in case of failure such as connection timeout
      geoRequest.onFailure { case _ =>
        log(req.toString(), msg, INFO, Some(logItem))
      }(geoIpCtx)

    }
  }
}
