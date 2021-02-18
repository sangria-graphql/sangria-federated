package core

import sangria.schema._

import ReviewService.ReviewService

object ReviewAPI {

  val Query = ObjectType(
    "Query",
    fields[ReviewService, Any](
      Field(
        name = "reviews",
        fieldType = ListType(Review.reviewSchema),
        resolve = _.ctx.getReviews)))
}
