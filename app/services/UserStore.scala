package services

import java.time.{Clock, Instant}
import java.util.UUID
import javax.inject._

import model.UserAccount
import play.api.Logger
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future


trait UserStore {

  def listUsers(): Seq[UserAccount]

  def getUser(email: String): Option[UserAccount]

  def upsertUser(userAccount: UserAccount): UserAccount

  def removeUser(email: String): Unit

  def removeToken(token: UUID): Unit

  def getUserByToken(token: UUID): Option[UserAccount]

  def createSession(email : String): UUID

  def validSession(token: UUID): Boolean

  def loginUser(username: String, password: String): Option[UserAccount]

}

@Singleton
class MemoryUserStore extends UserStore {

  var users: Map[String, UserAccount] = Map(
    "omelois@cakesolutions.net" -> UserAccount(UUID.randomUUID(),"Olivier","omelois@cakesolutions.net","Olivier",""),
    "jaakkop@cakesolutions.net" -> UserAccount(UUID.randomUUID(),"Jaakko","jaakkop@cakesolutions.net","Jaakko",""),
    "andrews@cakesolutions.net" -> UserAccount(UUID.randomUUID(),"Andrew","andrews@cakesolutions.net","Andrew",""),
    "michalj@cakesolutions.net" -> UserAccount(UUID.randomUUID(),"Michal","michalj@cakesolutions.net","Michal","")
  )
  var tokenCache: Map[UUID, String] = Map.empty

  def listUsers(): Seq[UserAccount] = users.values.toSeq

  def getUser(email: String): Option[UserAccount] =  users.get(email)

  def createSession(email : String): UUID = {
      tokenCache.find(_._2 == email).foreach {
        case (oldToken, _) => removeToken(oldToken)
      }
      val uuid = UUID.randomUUID()
      tokenCache = tokenCache.updated(uuid, email)
      uuid
    }

  def upsertUser(userAccount: UserAccount): UserAccount = {
    users = users.updated(userAccount.email, userAccount)
    userAccount
  }

  def removeUser(email: String) = users = users - email

  def removeToken(token: UUID) = tokenCache = tokenCache - token

  def getUserByToken(token: UUID) = tokenCache.get(token).flatMap(email => users.get(email))

  def validSession(token: UUID) = tokenCache.keys.exists(tk => tk == token)

  def loginUser(username: String, password: String): Option[UserAccount] = users.get(username).collect{case user if user.password.contains(password) => user}

}

