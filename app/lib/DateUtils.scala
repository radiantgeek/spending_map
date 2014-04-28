package lib

import org.joda.time.format.{ISODateTimeFormat, DateTimeFormat}
import org.joda.time.{DateTime, DateTimeZone}

object DateUtils {

  val isoFormat     = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss")
  val indexFmt      = DateTimeFormat.forPattern("yyyy.MM.dd")
  val rusFormat     = DateTimeFormat.forPattern("dd.MM.yyyy")
  val elasticFormat = DateTimeFormat.forPattern("yyyy-MM-dd")
  val stampFormat   = ISODateTimeFormat.dateTime()
  val jsDateFormat  = DateTimeFormat.forPattern("'new Date('yyyy, MM'-1', dd')'")

  def jsDate(time: DateTime) = jsDateFormat.print(time)
}
