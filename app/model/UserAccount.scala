package model

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.json.Json

case class UserAccount(
                        uuid: UUID,
                        login : String,
                        email: String,
                        name: String,
                        avatarURL : String,
                        password: Option[String] = None
                      )

case class UserRegistration(email: String, name: String, password: String, phoneNumber: Option[String])

case class UserUpdate(password: String, phoneNumber: Option[String], name: String)

object UserAccount {

  implicit val fmtUserAccount = Json.format[UserAccount]
  implicit val fmtUserRegistration = Json.format[UserRegistration]
  implicit val fmtUserUpdate = Json.format[UserUpdate]

}