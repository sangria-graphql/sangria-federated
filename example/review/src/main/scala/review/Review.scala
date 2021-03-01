package review

import sangria.schema._

case class Review(id: Int, key: Option[String] = None, state: State)

object Review {

  implicit val reviewSchema =
    ObjectType(
      "Review",
      fields[Unit, Review](
        Field("id", IntType, resolve = _.value.id),
        Field("key", OptionType(StringType), resolve = _.value.key),
        Field("state", State.stateSchema, resolve = _.value.state))
    )
}
