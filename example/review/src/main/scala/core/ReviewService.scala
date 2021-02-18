package core

object ReviewService {

  type ReviewService = Service

  trait Service {

    def getReviews: List[Review]
    def getReview(id: Int): Option[Review]
  }

  val inMemory: ReviewService = new Service {

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
