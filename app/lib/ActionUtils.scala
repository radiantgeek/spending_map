package lib

import play.api.mvc._
import play.api.libs.json._
import play.api.libs.json.Json.JsValueWrapper

import scala.concurrent._
import ExecutionContext.Implicits.global

import services.LogPoint

/**
 */
object ActionUtils {

  // ------------------------------------------------------------------------------------------------------

  def _async(f: => Future[JsValue]) = Action.async {
    f.map { json =>
      Results.Ok(Json.prettyPrint(json))
    }
  }


  // ------------------------------------------------------------------------------------------------------

  case class Logging[A](msg: String, params: (String, JsValueWrapper)*)(action: Action[A]) extends Action[A] {
    def apply(request: Request[A]): Future[SimpleResult] = {
      val time1 = System.currentTimeMillis
      action(request).map { response =>
        val time2 = System.currentTimeMillis
        LogPoint.logRequest(request, "", response.header.status, time2 - time1, params :_*)
        response
      }
    }
    lazy val parser = action.parser
  }

  // ------------------------------------------------------------------------------------------------------

  case class BasicAuth[A](username: String, password: String)(action: Action[A]) extends Action[A] {
    def apply(request: Request[A]): Future[SimpleResult] = {
      request.headers.get("Authorization").flatMap {
        authorization =>
          authorization.split(" ").drop(1).headOption.filter {
            encoded =>
              val code = new String(org.apache.commons.codec.binary.Base64.decodeBase64(encoded.getBytes))
              code.split(":").toList match {
                case u :: p :: Nil if u == username && password == p => true
                case _ => false
              }
          }.map(_ => action(request))
      }.getOrElse {
        Future {
          Results.Unauthorized.withHeaders("WWW-Authenticate" -> """Basic realm="Secured"""")
        }
      }
    }
    lazy val parser = action.parser
  }

  // ------------------------------------------------------------------------------------------------------

  case class Testing[A](f: (Request[A], SimpleResult)=>Unit)(action: Action[A]) extends Action[A] {
    def apply(request: Request[A]): Future[SimpleResult] = {
      action(request).map { response =>
        f(request, response)
        response
      }
    }
    lazy val parser = action.parser
  }

}
