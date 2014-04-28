package lib.ws

import play.api._
import play.api.libs.ws._
import play.api.libs.ws.WS._
import play.api.libs.json._

import scala.concurrent._
import java.net.URLEncoder
import com.fasterxml.jackson.core.JsonParseException
import lib.ws.WsService._
import services.LogPoint

// --------------------------------------------------------------------------------------------------------------

object WsService {
  //
  type TimeListener       = Long  =>  Unit
  //
  type RequestConfigurer  = WSRequestHolder =>  WSRequestHolder

  implicit val simpleRequestConfigurer: RequestConfigurer = { f => f }
}

trait WsService extends Encoder {

  val log: Logger

  // --------------------------------------------------------------------------------------------------------------

  def _cmd(cmd: String): WS.WSRequestHolder // Implementation example: = _request(s"$server/$api/$cmd")

  // --------------------------------------------------------------------------------------------------------------
  //  helpers

  def timedJson(a: => Future[Response])(implicit auth: Auth, executor: ExecutionContext): Future[JsValue] =
    asJson { _timing(logTimeListener) { a } }

  def asJson(a: => Future[Response])(implicit auth: Auth, executor: ExecutionContext): Future[JsValue] =
    _json { a } { (t: JsValue) => t }

  def withJson(ws: WS.WSRequestHolder): WS.WSRequestHolder =
    ws.withHeaders("Content-Type" -> "application/json")

  def withAuth(ws: WS.WSRequestHolder)(implicit auth: Auth): WS.WSRequestHolder =
    auth match {
      case AuthCredit(u, p, s)  => ws.withAuth(u, p, s)
      case MashapeAuth(key)     => ws.withHeaders(("X-Mashape-Authorization", key))
      case NoAuth   => ws
      case _        => ws
    }

  // --------------------------------------------------------------------------------------------------------------

  def logTimeListener(t: Long) = log.debug { s"response in $t ms" }

  def _timing[T](timeListener: TimeListener)(a: => Future[T])(implicit executor: ExecutionContext): Future[T] = {
    val time1 = System.currentTimeMillis
    a.map { r =>
      val time2 = System.currentTimeMillis
      timeListener(time2-time1)
      r
    }
  }

  // --------------------------------------------------------------------------------------------------------------
  //  utils

  def _request[T](link: String)(implicit auth: Auth): WS.WSRequestHolder = {
    log.debug { "URL = "+link }
    withAuth(WS.url(link))
  }

  def _make[T](req: => Future[Response])(worker: Response => T)
              (implicit auth: Auth, executor: ExecutionContext): Future[T] = {
    req.map { response =>
      worker(response)
    }
  }
  def _json[T](req: => Future[Response])(worker: JsValue => T)
              (implicit auth: Auth, executor: ExecutionContext): Future[T] = {
    _make(req) { (r: Response) =>
    //      log.trace(Json.prettyPrint(r.json))
      try {
        worker(r.json)
      } catch {
        case e: JsonParseException =>
          _printException(r)(e)
          throw e
      }
    }
  }

  def _printException(r: Response)(e: Throwable) = {
    val o = Json.obj("url" -> r.ahcResponse.getUri.toString, "text" -> r.body)
    val t = Json.arr("wsservice", "json", "exception")
    LogPoint.log("wsservice.exception", "json parse exception ", LogPoint.ERROR, Some(o), t) // TODO: very bad - close coupling
    log.error(s"json parse exception for ${r.ahcResponse.getUri} with text => ${r.body}")
  }
}

// --------------------------------------------------------------------------------------------------------------

trait Encoder {
  def encode(s: String) = URLEncoder.encode(s, "UTF-8")

  def encodeParams(params: (String, String)*): String = {
    params.map { case (p, v) => p+"="+encode(v)}.mkString("&")
  }

  def encodeURL(url: String): String = {
    url.split("/").filter(_.nonEmpty).map(encode).mkString("/").replace("+", "%20")
  }

}

