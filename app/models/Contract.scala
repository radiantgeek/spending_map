package models

import play.api.libs.json._ // JSON library
import play.api.libs.json.Reads._ // Custom validation helpers
import play.api.libs.functional.syntax._ // Combinator syntax

// ------------------------------------------------------------------------------------------------------

case class Contract(id:     String,
                    regNum: String,
                    price:  String,
                    signDate:     String,
                    protocolDate: String,
                    publishDate:  String,

                    supplier_inn: String,
                    supplier_kpp: String,
                    customer_regnum: String
)

object Contract {
  implicit val json = Json.format[Contract]

  val contractReads: Reads[Contract] = (
      (JsPath \ "id").read[String] and
      (JsPath \ "regNum").read[String] and
      (JsPath \ "price").read[String] and
      (JsPath \ "signDate").read[String] and
      (JsPath \ "protocalDate").read[String] and
      (JsPath \ "publishDate").read[String] and
      (JsPath \ "supplier" \ "inn").read[String] and
      (JsPath \ "supplier" \ "kpp").read[String] and
      (JsPath \ "customer" \ "regNum").read[String]
    )(Contract.apply _)

}

// ------------------------------------------------------------------------------------------------------

case class Customer(id: String,
                    orgType: String,
                    orgName: String,
                    factualAddress: String,
                    postAddress: String,
                    inn: String,
                    kpp: String
)

object Customer {
  implicit val json = Json.format[Customer]
}


// ------------------------------------------------------------------------------------------------------

case class Supplier(id: String,
                    orgType: String,
                    orgName: String,
                    factualAddress: String,
                    postAddress: String,
                    inn: String,
                    kpp: String
)

object Supplier {
  implicit val json = Json.format[Supplier]
}

// ------------------------------------------------------------------------------------------------------
