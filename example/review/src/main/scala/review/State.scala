package review

import sangria.schema._

case class State(id: Int)

object StateGraphQLSchema {

  import sangria.federation.v2.Directives._

  val schema =
    ObjectType(
      "State",
      fields[Unit, State](
        Field[Unit, State, Int, Int](
          name = "id",
          fieldType = IntType,
          resolve = _.value.id,
          astDirectives = Vector(External)))
    ).withDirectives(Key("id"), Extends)
}
