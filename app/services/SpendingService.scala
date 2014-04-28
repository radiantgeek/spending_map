package services

import lib._
import lib.ws._

import play.api._
import play.api.libs.json._
import play.api.libs.ws._
import play.api.libs.ws.WS.WSRequestHolder

import scala.concurrent._
import models._

// ----------------------------------------------------------------------

object ClearSpending extends SpendingService with Conf {

  lazy val log      = Logger("services.spending")

  implicit val auth: Auth = MashapeAuth(key)
  implicit val ctx  = Contexts.spendingRetrieveCtx

  lazy val view     = conf.getString("spending.server").get
  lazy val server   = conf.getString("mashape.server").get
  lazy val api      = conf.getString("mashape.api").get
  lazy val key      = conf.getString("mashape.key").get
  lazy val timeout  = conf.getInt("mashape.timeout").get

  // ----------------------------------------------------------------------

  def contractView(regNum: String)            = s"$view/contract/$regNum/"
  def customerView(regNum: String)            = s"$view/customer/$regNum/"
  def supplierView(inn: String, kpp: String)  = s"$view/supplier/inn=$inn&kpp=$kpp"

  // ----------------------------------------------------------------------

  def logListener(r: WSRequestHolder, _tags: String*)(time: Long) = {
    val q = r.queryString.toSeq.map {
      t => (t._1, JsString(t._2.mkString(", ")))
    }
    val o = Json.obj(
      "url" -> r.url,
      "duration_ms" -> time,
      "query" -> r.queryString.toString,
      "params" -> JsObject(q)
    )

    LogPoint.log("clearspending.request", "request", LogPoint.INFO, Some(o), _logTags(_tags:_*))
  }
  def logException(msg: String, _tags: String*)(e: Throwable) = {
    val o = Json.obj(
      "exception.message" -> e.getMessage
//      "exception.stack" -> e.getStackTrace
    )
    LogPoint.log("clearspending.request", "exception", LogPoint.ERROR, Some(o), _logTags(_tags:_*))
  }

  def _logTags(_tags: String*) = {
    val default = Seq("clearspending")
    val t = (default++_tags).toSeq.map(JsString)
    Json.arr(t)
  }

  // ----------------------------------------------------------------------

  def _cmd(cmd: String): WS.WSRequestHolder = _request(s"$server$api$cmd")

}

// ----------------------------------------------------------------------

trait SpendingService extends WsService {
  implicit val auth: Auth
  implicit val ctx:  ExecutionContext
  implicit val timeout:  Int

  def logListener(req: WSRequestHolder, tags: String*)(time: Long)
  def logException(msg: String, _tags: String*)(e: Throwable)

  // ----------------------------------------------------------------------

  def contract(regNum: String)            = _extractResult("contracts") {
    contractsGet("regnum" -> regNum)
  }
  def customer(regNum: String)            = _extractResult("customers") {
    customersGet("spzregnum" -> regNum)
  }
  def supplier(inn: String, kpp: String)  = _extractResult("suppliers") {
    suppliersGet("inn" -> inn, "kpp" -> kpp)
  }

  // ----------------------------------------------------------------------

  def contractsSelect(params: (String, String)*) = _search("contracts/select", params: _*)
  def contractsGet(params: (String, String)*)    = _search("contracts/get",    params: _*)
  def contractsSearch(params: (String, String)*) = _search("contracts/search", params: _*)

  def customersSelect(params: (String, String)*) = _search("customers/select", params: _*)
  def customersGet(params: (String, String)*)    = _search("customers/get",    params: _*)
  def customersSearch(params: (String, String)*) = _search("customers/search", params: _*)

  def suppliersSelect(params: (String, String)*) = _search("suppliers/select", params: _*)
  def suppliersGet(params: (String, String)*)    = _search("suppliers/get",    params: _*)
  def suppliersSearch(params: (String, String)*) = _search("suppliers/search", params: _*)

  def dictOPF(params: (String, String)*)     = _search("opf/select", params: _*)
  def dictRegion(params: (String, String)*)  = _search("regions/select", params: _*)
  def dictPlacing(params: (String, String)*) = _search("placing/select", params: _*)
  def dictBudgetL(params: (String, String)*) = _search("budgetlevels/select", params: _*)

  // ----------------------------------------------------------------------
  // utils

  def _search(cmd: String, params: (String, String)*) = {
    val p = withJson(_cmd(cmd+"/").withQueryString(params: _ *))
    log.debug { s"cmd: $cmd, search query: ${p.queryString}" }

    asJson { _timing( logListener(p, cmd) ) { p.withRequestTimeout(timeout).get() } }
  }

  def _extractResult(name: String)(res: => Future[JsValue]): Future[JsValue] = {
    _catchFutureException(s"try extract '$name' collection") {
      res.map { js =>
          (js \ name \ "data").as[JsArray].value.head
      }
    }
  }

  def _catchFutureException[T](msg: String, _tags: String*)(res: => Future[T]): Future[T] = {
    res onFailure { case e: Throwable =>
      logException(msg, _tags :_*)(e)
    }
    res
  }
  def _recoverFutureException[T](msg: String, _tags: String*)(res: => Future[T]): Future[Option[T]] = {
    res.map {
      Some(_)
    }.recover { case e: Throwable =>
      logException(msg, _tags :_*)(e)
      None
    }
  }

  def _catchException[T](msg: String, _tags: String*)(res: => T): T = {
    try {
      res
    } catch {
       case e: Throwable => {
         logException(msg, _tags :_*)(e)
         throw e
       }
    }
  }

}

