package review

import scala.concurrent.ExecutionContext

case class ReviewContext(review: ReviewService, ec: ExecutionContext)
