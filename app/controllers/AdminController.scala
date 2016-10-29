package controllers

import com.google.inject.Inject
import play.api.Logger
import play.api.mvc.AnyContent
import services.UserStore


class AdminController @Inject()(val userStore: UserStore) extends Authorization {

  val logger: Logger = Logger(this.getClass)

  def listUsers = withUser { (r: UserSessionRequest[AnyContent]) =>
    Ok(views.html.users(r.userAccount, userStore.listUsers()))

  }

}
