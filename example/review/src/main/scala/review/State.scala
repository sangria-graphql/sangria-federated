package review

import sangria.schema._

case class State(id: Int)

object State {

  import sangria.federation.Directives._

  implicit val stateSchema =
    ObjectType(
      "State",
      fields[Unit, State](
        Field[Unit, State, Int, Int](name = "id", fieldType = IntType, resolve = _.value.id)
          .copy(astDirectives = Vector(External)))
    ).copy(astDirectives = Vector(Key("id"), Extends))
}
