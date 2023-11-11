package review

import common.CustomDirectives
import sangria.execution.deferred.{Fetcher, HasId}
import sangria.schema.{
  Argument,
  Field,
  IntType,
  ListInputType,
  ListType,
  ObjectType,
  OptionType,
  StringType,
  fields
}

object ReviewGraphQLSchema {
  val ReviewType: ObjectType[Unit, Review] =
    ObjectType(
      "Review",
      fields[Unit, Review](
        Field("id", IntType, resolve = _.value.id),
        Field(
          "key",
          OptionType(StringType),
          resolve = _.value.key,
          astDirectives = Vector(CustomDirectives.Feature("review-key"))),
        Field("state", StateGraphQLSchema.schema, resolve = _.value.state)
      )
    )

  implicit val reviewId: HasId[Review, Int] = _.id
  val reviews: Fetcher[ReviewContext, Review, Review, Int] = Fetcher {
    (ctx: ReviewContext, ids: Seq[Int]) =>
      ctx.review.getReviews(ids)(ctx.ec)
  }

  private val ids: Argument[Seq[Int]] =
    Argument("ids", ListInputType(IntType)).asInstanceOf[Argument[Seq[Int]]]
  val Query: ObjectType[ReviewContext, Any] = ObjectType(
    "Query",
    fields[ReviewContext, Any](
      Field(
        name = "reviews",
        fieldType = ListType(ReviewGraphQLSchema.ReviewType),
        arguments = List(ids),
        resolve = ctx => reviews.deferSeqOpt(ctx.arg(ids))
      ))
  )
}
