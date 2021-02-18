package core

import core.StateService.StateService
import sangria.federation._
import io.circe.Json
import io.circe.generic.semiauto._
import sangria.schema._

case class State(id: Int, key: String, value: String)

object State {

  case class StateArg(id: Int)

  implicit val decoder: Decoder[Json, StateArg] = deriveDecoder[StateArg].decodeJson(_)

  def stateResolver(env: StateService) = EntityResolver[StateService, Json, State, StateArg](
    __typeName = "State",
    arg => env.getState(arg.id))

  implicit val stateSchema =
    ObjectType(
      "State",
      fields[Unit, State](
        Field("id", IntType, resolve = _.value.id),
        Field("key", StringType, resolve = _.value.key),
        Field("value", StringType, resolve = _.value.value))
    ).copy(astDirectives = Vector(Directives.Key("id")))
}
