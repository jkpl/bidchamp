package services

import java.time.{Clock, Instant}
import java.util.UUID
import javax.inject._

import model.UserAccount
import play.api.Logger
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future


trait UserStore {

  def getUser(userId: UUID): Option[UserAccount]

  def upsertUser(userAccount: UserAccount): UserAccount

  def removeUser(userId: UUID)
}

@Singleton
class MemoryUserStore extends UserStore {

  var users: Map[UUID, UserAccount] = Map.empty

  def getUser(userId: UUID): Option[UserAccount] =  users.get(userId)

  def upsertUser(userAccount: UserAccount): UserAccount = {
    users = users.updated(userAccount.uuid, userAccount)
    userAccount
  }

  def removeUser(userId: UUID) = users = users - userId

}

