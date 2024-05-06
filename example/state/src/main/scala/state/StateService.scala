package state

import scala.concurrent.{ExecutionContext, Future}

trait StateService {

  def getStates(ids: Seq[Int])(implicit ec: ExecutionContext): Future[Seq[State]]
}

object StateService {

  val inMemory: StateService = new StateService {
    def stateFor(id: Int): State = State(
      id = id,
      key = s"key$id",
      value = s"value$id"
    )

    override def getStates(ids: Seq[Int])(implicit ec: ExecutionContext): Future[Seq[State]] =
      Future(ids.map(stateFor))
  }
}
