package services

import lib.DateUtils

/**
 *
 */
object ParametersMapping {

  // ----------------------------------------------------------------------

  def paramToElastic(params: (String, String)*): Seq[(String, String)] = {
    params.map { case (param, value) =>
      val _param = mapToElastic(param)
      val _value = convertValue(value, param)
      (_param, _value)
    }
  }

  // ----------------------------------------------------------------------

  val rusFormat     = DateUtils.rusFormat
  val elasticFormat = DateUtils.elasticFormat

  def elasticTime(s: String): String = {
    elasticFormat.print( rusFormat.parseDateTime(s) )
  }

  // ----------------------------------------------------------------------

  val mapToElastic = Map(
    "customerinn"     -> "customer.inn",
    "customerkpp"     -> "customer.kpp",
    "customerregion"  -> "regionCode",
    "supplierinn"     -> "suppliers.supplier.inn",
    "supplierkpp"     -> "suppliers.supplier.kpp",
    "okdp"            -> "products.product.OKDP",
    "daterange"       -> "signDate",
    "pricerange"      -> "price"
    //    "budgetlevel"     -> "",
    //    "placing"         -> ""
    //    "productsearch"
  )

  def convertValue(value: String, param: String) = {
    var parsed = value match {
      case r"(.+)$f-(.+)$t" => (f, t)
      case f: String => f
    }
    // hack for datetime
    if (param == "daterange") {
      parsed match {
        case (f: String, t: String) => parsed = (elasticTime(f), elasticTime(t))
      }
    }
    parsed match {
      case (f, t) => s"[$f TO $t]"
      case f: String => f
    }
  }
  // ----------------------------------------------------------------------

  implicit class Regex(sc: StringContext) {
    def r = new scala.util.matching.Regex(sc.parts.mkString, sc.parts.tail.map(_ => "x"): _*)
  }


}
