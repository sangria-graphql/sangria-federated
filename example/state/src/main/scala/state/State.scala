package state

import sangria.federation._
import io.circe.Json
import io.circe.generic.semiauto._
import sangria.schema._

case class State(id: Int, key: String, value: String)

object State {

  case class StateArg(id: Int)

  implicit val decoder: Decoder[Json, StateArg] = deriveDecoder[StateArg].decodeJson(_)

  def stateResolver(env: StateEnv) = EntityResolver[StateEnv, Json, State, StateArg](
    __typeName = "State",
    arg => env.service.getState(arg.id))

  implicit val stateSchema =
    ObjectType(
      "State",
      fields[Unit, State](
        Field("id", IntType, resolve = _.value.id),
        Field("key", StringType, resolve = _.value.key),
        Field("value", StringType, resolve = _.value.value))
    ).copy(astDirectives = Vector(Directives.Key("id")))
}
