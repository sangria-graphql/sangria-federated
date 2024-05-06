package review

import scala.concurrent.{ExecutionContext, Future}

trait ReviewService {

  def getReviews(ids: Seq[Int])(implicit ec: ExecutionContext): Future[Seq[Review]]
}

object ReviewService {

  val inMemory: ReviewService = new ReviewService {
    def reviewFor(id: Int): Review = Review(
      id = id,
      key = Some(s"key$id"),
      State(id = 0)
    )

    override def getReviews(ids: Seq[Int])(implicit ec: ExecutionContext): Future[Seq[Review]] =
      Future(ids.map(reviewFor))
  }
}
