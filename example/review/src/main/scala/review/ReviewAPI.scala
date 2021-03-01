package review

import sangria.schema._

object ReviewAPI {

  val Query = ObjectType(
    "Query",
    fields[ReviewService, Any](
      Field(
        name = "reviews",
        fieldType = ListType(ReviewGraphQLSchema.schema),
        resolve = _.ctx.getReviews)))
}
