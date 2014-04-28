package models

// See API for more information:
//  https://www.mashape.com/infoculture/clearspending-ru-as-russian-government-spending

/**
 * 
 */
case class ContractReq(
  page:         String,
  perpage:      Option[String],   // max 50
  returnfields: Option[String],   // list

  sort:         Option[String],   // ! ONLY FOR `select`
  productsearch:Option[String],   // ! ONLY FOR `search`

  customerinn:  Option[String],   // ИНН поставщика     // Example: 6504016811
  customerkpp:  Option[String],   // КПП заказчика 	    // Example: 650401001
  supplierinn:  Option[String],   // ИНН поставщика     // Example: 6504016811
  supplierkpp:  Option[String],   // КПП поставщика 	   / Example: 650401001
  okdp:         Option[String],   // Код продукции по ОКДП 	// Example: 1520110
  budgetlevel:  Option[String],   // Уровень бюджета 	      // Example: 02
  customerregion: Option[String], // Код региона заказчика 	// Example: 65
  daterange:    Option[String],   // Диапазон дат заключения контракта 	// Example: 27.01.2011-01.02.2011
  pricerange:   Option[String],   // Диапазон сумм контракта 	          // Example: 300000-400000
  placing:      Option[String]    // Код способа размещения заказа 	    // Example: 5
)

case class CustromerReq(
  page: String
)

case class SupplierReq(
  page: String
)