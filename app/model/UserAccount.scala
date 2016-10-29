package model

import java.util.UUID

import org.joda.time.DateTime
import play.api.libs.json.Json

case class UserAccount(
                        uuid: UUID,
                        email: String,
                        name: Option[String],
                        phoneNumber: Option[String],
                        created: DateTime,
                        passwordInfo: String,
                        failureAttempt: Int
                      )

case class UserRegistration(email: String, name: String, password: String, phoneNumber: Option[String])


object UserAccount {

  implicit val fmtUserAccount = Json.format[UserAccount]
  implicit val fmtUserRegistration = Json.format[UserRegistration]

}