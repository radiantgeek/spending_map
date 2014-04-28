package lib.html

case class Page(title: String, topic: String="index",
                brand: String = "Куда уходят деньги?") {

  def index           = copy(topic = "index")
  def new_search      = copy(topic = "new_search")
  def results         = copy(topic = "results")
  def about           = copy(topic = "about")

}

