package controllers

import javax.inject._

import play.api.mvc._
import services.UserStore

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(val userStore : UserStore) extends Controller with Authorization {

  /**
   * Create an Action to render an HTML page with a welcome message.
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index = withUser.apply{
    Ok(views.html.index("Your new application is ready."))
  }


}
