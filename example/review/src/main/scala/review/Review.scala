package review

import sangria.schema._
import common.Directives.Feature

case class Review(id: Int, key: Option[String] = None, state: State)

object ReviewGraphQLSchema {

  val schema =
    ObjectType(
      "Review",
      fields[Unit, Review](
        Field("id", IntType, resolve = _.value.id),
        Field(
          "key",
          OptionType(StringType),
          resolve = _.value.key,
          astDirectives = Vector(Feature("review-key"))),
        Field("state", StateGraphQLSchema.schema, resolve = _.value.state)
      )
    )
}
