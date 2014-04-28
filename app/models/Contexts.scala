package models

import scala.concurrent.ExecutionContext
import play.libs.Akka

object Contexts {

  val loggerCtx           = Akka.system.dispatchers.lookup("logstash-context")
  val cacherCtx           = Akka.system.dispatchers.lookup("cacher-context")
  val cacherWriteCtx      = Akka.system.dispatchers.lookup("cacher-write-context")
  val elasticCtx          = Akka.system.dispatchers.lookup("elastic-context")

  val spendingRetrieveCtx = Akka.system.dispatchers.lookup("spending-retrieve-context")
  val geoipCtx            = Akka.system.dispatchers.lookup("geo-ip-context")

  val geocoderCtx         = Akka.system.dispatchers.lookup("common-geocoder-context")
  val googleGeocoderCtx   = Akka.system.dispatchers.lookup("google-geocoder-context")
  val yandexGeocoderCtx   = Akka.system.dispatchers.lookup("yandex-geocoder-context")
  val osmGeocoderCtx      = Akka.system.dispatchers.lookup("osm-geocoder-context")

}

