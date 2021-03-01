package state

trait StateService {

  def getStates: List[State]
  def getState(id: Int): Option[State]
}

object StateService {

  val inMemory: StateService = new StateService {

    val states = State(
      id = 0,
      key = "key0",
      value = "initial"
    ) :: Nil

    def getStates: List[State] = states

    def getState(id: Int): Option[State] =
      states.find(_.id == id)
  }
}
