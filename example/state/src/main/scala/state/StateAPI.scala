package state

import sangria.schema._

object StateAPI {

  val Query = ObjectType(
    "Query",
    fields[StateEnv, Any](
      Field(
        name = "states",
        fieldType = ListType(State.stateSchema),
        resolve = _.ctx.service.getStates)))
}
