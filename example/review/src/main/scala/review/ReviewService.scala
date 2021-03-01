package review

trait ReviewService {

  def getReviews: List[Review]
  def getReview(id: Int): Option[Review]
}

object ReviewService {

  val inMemory: ReviewService = new ReviewService {

    val reviews = Review(
      id = 0,
      key = Some("review 0"),
      State(id = 0)
    ) :: Review(
      id = 1,
      key = Some("review 1"),
      State(id = 0)
    ) :: Nil

    def getReviews: List[Review] = reviews

    def getReview(id: Int): Option[Review] =
      reviews.find(_.id == id)
  }
}
