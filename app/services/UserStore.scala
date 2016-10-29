package services

import java.time.{Clock, Instant}
import java.util.UUID
import javax.inject._

import model.UserAccount
import play.api.Logger
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future


trait UserStore {

  def getUser(email: String): Option[UserAccount]

  def upsertUser(userAccount: UserAccount): UserAccount

  def validPassword(email: String, password: String): Option[UUID]

  def removeUser(email: String): Unit

  def removeToken(token: UUID): Unit

  def getUserByToken(token: UUID): Option[UserAccount]

  def validSession(token: UUID): Boolean
}

@Singleton
class MemoryUserStore extends UserStore {

  var users: Map[String, UserAccount] = Map.empty

  var tokenCache: Map[UUID, String] = Map.empty

  def getUser(email: String): Option[UserAccount] =  users.get(email)

  def validPassword(email: String, password: String): Option[UUID] = users.get(email)
    .map { case a if a.passwordInfo == password =>
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
}

