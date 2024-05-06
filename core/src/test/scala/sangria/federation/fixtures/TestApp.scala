package sangria.federation.fixtures

import sangria.execution.deferred.HasId
import sangria.federation.fixtures.TestApp.FakeDB
import sangria.federation.v2.Directives.Key
import sangria.schema.{Field, IntType, ListType, ObjectType, OptionType, Schema, StringType, fields}

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class TestApp {
  val db: FakeDB = FakeDB()
  val ctx: TestApp.Context = TestApp.Context(db)
}

object TestApp {
  val missingStateId: Int = 42

  case class Context(db: FakeDB)

  case class FakeDB() {
    val stateDbCalled = new AtomicInteger(0)
    def statesByIds(ids: Seq[Int]): Future[Seq[State]] = Future {
      stateDbCalled.incrementAndGet()
      ids.filterNot(_ == missingStateId).map(id => State(id, s"mock state $id"))
    }

    val reviewDbCalled = new AtomicInteger(0)
    def reviewsByIds(ids: Seq[Int]): Future[Seq[Review]] = Future {
      reviewDbCalled.incrementAndGet()
      ids.map(id => Review(id, s"mock review $id"))
    }
  }

  case class State(id: Int, value: String)
  object State {
    implicit val hasId: HasId[State, Int] = _.id
  }

  val StateType: ObjectType[Context, State] = ObjectType(
    "State",
    fields[Context, State](
      Field("id", IntType, resolve = _.value.id),
      Field("value", OptionType(StringType), resolve = _.value.value))).withDirective(Key("id"))

  case class Review(id: Int, value: String)
  object Review {
    implicit val hasId: HasId[Review, Int] = _.id
  }

  // Review GraphQL Model
  val ReviewType: ObjectType[Unit, Review] = ObjectType(
    "Review",
    fields[Unit, Review](
      Field("id", IntType, resolve = _.value.id),
      Field("value", OptionType(StringType), resolve = _.value.value))).withDirective(Key("id"))

  val Query: ObjectType[Context, Any] = ObjectType(
    "Query",
    fields[Context, Any](
      Field(name = "states", fieldType = ListType(StateType), resolve = _ => Nil),
      Field(name = "reviews", fieldType = ListType(ReviewType), resolve = _ => Nil)
    )
  )

  val schema: Schema[Context, Any] = Schema(Query)

}
