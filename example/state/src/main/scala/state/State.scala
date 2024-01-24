package state

import sangria.federation.v1._
import io.circe.Json
import io.circe.generic.semiauto._
import sangria.schema._

case class State(id: Int, key: String, value: String)

object StateGraphQLSchema {

  case class StateArg(id: Int)

  implicit val decoder: Decoder[Json, StateArg] = deriveDecoder[StateArg].decodeJson(_)

  val stateResolver: EntityResolver[StateService, Json] =
    EntityResolver[StateService, Json, State, StateArg](
      __typeName = "State",
      (ids, ctx) => ctx.ctx.getStates(ids.map(_.id)),
      s => StateArg(s.id)
    )

  val schema: ObjectType[Unit, State] =
    ObjectType(
      "State",
      fields[Unit, State](
        Field("id", IntType, resolve = _.value.id),
        Field("key", StringType, resolve = _.value.key),
        Field("value", StringType, resolve = _.value.value))
    ).withDirective(Directives.Key("id"))
}
