package state

import sangria.schema._

object StateAPI {

  val Query = ObjectType(
    "Query",
    fields[StateService, Any](
      Field(
        name = "states",
        fieldType = ListType(StateGraphQLSchema.schema),
        resolve = _.ctx.getAllStates)))
}
