package controllers

import com.google.inject.Inject
import play.api.Logger
import play.api.mvc.Action
import services.UserStore


class AdminController @Inject()(val userStore: UserStore) extends Authorization {

  val logger: Logger = Logger(this.getClass)

  def listUsers = Action {

    Ok(views.html.users(userStore.listUsers()))

  }

}
