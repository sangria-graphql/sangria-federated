package review

import sangria.schema._

object ReviewAPI {

  val Query = ObjectType(
    "Query",
    fields[ReviewEnv, Any](
      Field(
        name = "reviews",
        fieldType = ListType(Review.reviewSchema),
        resolve = _.ctx.service.getReviews)))
}
