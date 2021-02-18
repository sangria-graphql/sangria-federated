package core

import sangria.schema._

import StateService.StateService

object StateAPI {

  val Query = ObjectType(
    "Query",
    fields[StateService, Any](
      Field(name = "states", fieldType = ListType(State.stateSchema), resolve = _.ctx.getStates)))
}
