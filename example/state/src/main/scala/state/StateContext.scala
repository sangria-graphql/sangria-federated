package state

import scala.concurrent.ExecutionContext

case class StateContext(state: StateService, ec: ExecutionContext)
