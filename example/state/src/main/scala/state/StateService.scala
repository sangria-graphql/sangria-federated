package state

import scala.concurrent.Future

trait StateService {

  def getAllStates: Future[List[State]]
  def getStates(ids: Seq[Int]): Future[List[State]]
  def getState(id: Int): Future[Option[State]]
}

object StateService {

  val inMemory: StateService = new StateService {

    val states = State(
      id = 0,
      key = "key0",
      value = "initial"
    ) :: Nil

    override def getAllStates: Future[List[State]] = Future.successful(states)

    def getStates(ids: Seq[Int]): Future[List[State]] =
      Future.successful(states.filter(s => ids.contains(s.id)))

    def getState(id: Int): Future[Option[State]] =
      Future.successful(states.find(_.id == id))
  }
}
