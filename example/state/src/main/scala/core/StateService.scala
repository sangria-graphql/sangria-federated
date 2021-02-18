package core

object StateService {

  type StateService = Service

  trait Service {

    def getStates: List[State]
    def getState(id: Int): Option[State]
  }

  val inMemory: StateService = new Service {

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
