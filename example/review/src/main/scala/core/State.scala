package core

import io.circe.generic.semiauto.deriveDecoder
import sangria.schema._
import io.circe.Decoder

case class State(id: Int)

object State {

  import sangria.federation.Directives._

  implicit val decoder: Decoder[State] = deriveDecoder

  implicit val stateSchema =
    ObjectType(
      "State",
      fields[Unit, State](
        Field[Unit, State, Int, Int](name = "id", fieldType = IntType, resolve = _.value.id)
          .copy(astDirectives = Vector(External)))
    ).copy(astDirectives = Vector(Key("id"), Extends))
}
