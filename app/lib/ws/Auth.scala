package lib.ws

import play.api._
import com.ning.http.client.Realm


// --------------------------------------------------------------------------------------------------------------

object Auth {

  def no = NoAuth

  def basic(user: String, pass: String) = AuthCredit(user, pass, Realm.AuthScheme.BASIC)
  
  def basicConfig(userConfig: String, passConfig: String)(implicit conf: Configuration) =
    basic(conf.getString(userConfig).get, conf.getString(passConfig).get)

}

// --------------------------------------------------------------------------------------------------------------

sealed class  Auth

case class    AuthCredit(user: String, pass: String, schema: Realm.AuthScheme) extends Auth
case class    MashapeAuth(key: String) extends Auth
case object   NoAuth extends Auth

