package lib.html.tags

import play.api.templates.Html

case class Tab(id: String, title: String, body: Html, active: Boolean=false) {

}
