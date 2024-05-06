package sangria.federation

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

object FutureAwaits {

  val DefaultTimeout: Duration = 20.seconds

  /** Block until a Promise is redeemed.
    */
  def await[T](future: Future[T])(implicit atMost: Duration = DefaultTimeout): T =
    try Await.result(future, atMost)
    // fill in current stack trace to be able to tell which await call failed
    catch { case e: Throwable => throw e.fillInStackTrace() }

}
