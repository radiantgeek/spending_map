package controllers

import play.api._
import play.api.mvc._
import play.api.cache._
import play.api.libs.json._
import play.api.templates._

import scala.concurrent._
import ExecutionContext.Implicits.global
import play.api.Play.current

import scala.io.Source
import services._
import lib.html.HtmlStream
import lib.html.HtmlStream._

import lib.ActionUtils._
import models.{PublishRequest, SearchRequest}

// ----------------------------------------------------------------------

object Application extends Controller {

  // ----------------------------------------------------------------------
  val page = lib.html.Page("Where does money go? Moscow edition")

  def index = Logging("index page") {
    Action {
      Ok(views.html.index(page.index))
    }
  }

  def about = Logging("about page") {
    Action {
      Ok(views.html.about(page.about))
    }
  }

  def histo = Cached("histo") { _async {
    ServicePoint._histo
  }}

  def search_form = Logging("search form") {
    Action {
      val data = SearchRequest.DEFAULT
      val f = SearchRequest._form.fill(data)

      Ok(views.html.search_form(page.new_search, f))
    }
  }

  def search_form_from(id: String) = Logging("copy form") {
    Action.async {
      ServicePoint.find_search(id).recover {
        case e => SearchRequest.DEFAULT
      }.map { data =>
        val f = SearchRequest._form.fill(data)

        Ok(views.html.search_form(page.new_search, f))
      }
    }
  }

  def search_start = Logging("start search") {
    Action.async { implicit req =>
      val fa  = SearchRequest._form.bindFromRequest
      if (fa.errors.length > 0) {
        Future {
          Ok(views.html.search_form(page.new_search, fa))
        }
      } else {
        val d = fa.get.copy(published = None, approved = None) // securi
        ServicePoint.save_search(d).map {
          js =>
            Redirect(routes.Application.results((js \ "_id").as[String]))
        }
      }
    }
  }

  // ----------------------------------------------------------------------

  def result_publish = Logging("result publish") {
    Action { implicit req =>
      val fa  = PublishRequest._publish_form.bindFromRequest
      var res = fa.errors.isEmpty
      if (res) {
        ServicePoint.find_search(fa.get.id).recover { case e =>
          res = false
        }.map { data =>
          ServicePoint.update_search(fa.get.id, fa.get.text)
        }
      }
      Ok(Json.toJson(Json.obj("success" -> res)))
    }
  }

  def result_list = Logging("result list page") {
    Action {
      Ok(views.html.result_list(page.results))
    }
  }

  // ----------------------------------------------------------------------

  def results(id: String) = Logging("show results for "+id, "search_id" -> id) {
    Action.async {
      ServicePoint.find_search(id).map { search =>
        Ok(views.html.search_result(page.results, search))
      }
    }
  }

  def req(id: String, page: Long) = _async {
    for {
      search  <- ServicePoint.find_search(id)
      results <- ServicePoint.summarize(page, search)
    } yield { results }
  }

  // ----------------------------------------------------------------------


  def addr = Action {
    val file = Source.fromFile("data/filtered.csv")
    val read = file.getLines() // .slice(0, 10)

    val list = read.toList.map { s =>
      val a = OsmLoader.parseAddress(s)

      ServicePoint.osmGeoAddress(a.address) {
        Thread.sleep(400) // small timeout, "wait" for previous save operations
        Json.toJson(a)
      }.map { js =>
        Html( (js \ "address") + "</br>" )
      }
    }
    val printed = list.map( HtmlStream(_) )
    Ok.chunked(HtmlStream.interleave(printed))
  }

  // ----------------------------------------------------------------------

  def contract(regnum: String) = _async {
    ServicePoint.contract(regnum)
  }

  def customer(regnum: String) = _async {
    ServicePoint.customer(regnum)
  }

  def supplier(inn: String, kpp: String) = _async {
    ServicePoint.supplier(inn, kpp)
  }

  def customer_address(regnum: String) = _async {
    for {
      j <- ServicePoint.customer(regnum)
      s <- ServicePoint.geocode((j \ "factualAddress" \ "addressLine").as[String])
    } yield {
      Json.arr(s, Json.obj("separator" -> "-------"), j)
    }
  }
  def supplier_address(inn: String, kpp: String) = _async {
    for {
      j <- ServicePoint.supplier(inn, kpp)
      s <- ServicePoint.geocode((j \ "factualAddress").as[String])
    } yield {
      Json.arr(s, Json.obj("separator" -> "-------"), j)
    }
  }


  // ------------------------------------------------------------------------------------------------------

  def javascriptRoutes = Action { implicit request =>
    import routes.javascript._
    Ok(
      Routes.javascriptRouter("jsRoutes")(
        routes.javascript.Application.req,
        routes.javascript.Application.result_publish,
        routes.javascript.Application.contract,
        routes.javascript.Application.customer,
        routes.javascript.Application.supplier
      )
    ).as("text/javascript")
  }


}