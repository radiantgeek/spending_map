package services

import play.api.libs.json.Json

case class Address(city: String, streetType: String, streetName: String, home: String,
                   lat: Double, lon: Double,
                   address: String, osmId: Long, original: String)

object Address {
  implicit val format = Json.format[Address]
}

/**
  */
object OsmLoader {
  val STREET_TYPES = "улица|проезд|тупик|бульвар|переулок|площадь|шоссе|проспект|набережная|аллея"

  def filterHome(h: String) = {
    h.toLowerCase.
      replaceAll("(\\s|,)+",  " ").
      replaceAll("с(тр.\\s*)?(\\d+)",   " строение $2").
      replaceAll("вл\\.?(\\d+)",          " владение $1").
      replaceAll("в(лад\\.\\s*)?(\\d+)",  " владение $2").
      replaceAll("к\\.\\s*(\\d+)",        " корпус $1").
      replaceAll("к(ор\\.\\s*)?(\\d+)",   " корпус $2").
      replaceAll("k(\\d+)",   " корпус $1").
      replaceAll("c(\\d+)",   " строение $1").
      replaceAll("^д(ом|\\.)?\\s*(\\d+)",   "$2").
      replaceAll("(\\d+)\\s+([а-я])\\s+",   "$1$2").
//      replaceAll("^\\d+[а-я]?((/|-)\\d+[а-я]?)?\\s*",  ""). // for debugging (remove first digits)
      replaceAll("(\\s|,)+",  " ")
  }

  def fullStreet(s: String) = {
    (" " + s + " ").
      replaceAll(",",  " , ").
      replaceAll(" ул\\.\\s*",    "улица ").
      replaceAll(" ш\\.\\s*",     "шоссе ").
      replaceAll(" наб\\.\\s*",   "набережная ").
      replaceAll(" ал\\.\\s*",    "аллея ").
      replaceAll(" пр\\.\\s*",    "проспект ").
      replaceAll(" пркт\\.\\s*",  "проспект ").
      replaceAll(" пер\\.\\s*",   "переулок ").
      replaceAll("\\s+",  " ").trim
  }

  def parseStreet(s: String) = {
    val f = fullStreet(s)

    var sType = f.replaceAll(s".*($STREET_TYPES).*", "$1")
    val sName = f.replaceAll(STREET_TYPES, "").trim
    if (sName == sType) sType = ""
    (sType, sName)
  }

  def parseAddress(s: String) = {
    val arr = s.split(";").map(_.replace("'", ""))
    val city  = arr(0)
    val (sType, sName) = parseStreet(arr(1))
    val home  = filterHome(arr(2))
    val id    = arr(4).toLong
    val lat   = arr(6).toDouble
    val lon   = arr(7).toDouble

    val addr = s"$sType, $sName, $home"
    Address(city, sType, sName, home, lat, lon, addr, id, s)
  }
}
