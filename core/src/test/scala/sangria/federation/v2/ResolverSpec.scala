package sangria.federation.v2

import io.circe.Json
import io.circe.generic.semiauto.deriveDecoder
import io.circe.parser.parse
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sangria.ast
import sangria.execution.Executor
import sangria.execution.deferred.{Deferred, DeferredResolver, Fetcher}
import sangria.federation.FutureAwaits.await
import sangria.federation.fixtures.TestApp
import sangria.marshalling.InputUnmarshaller
import sangria.marshalling.queryAst.queryAstResultMarshaller
import sangria.parser.QueryParser
import sangria.renderer.QueryRenderer
import sangria.schema._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class ResolverSpec extends AnyWordSpec with Matchers {
  import ResolverSpec._
  "Resolver" should {
    "fetch several entities of same type in one call using DeferredValue" in FetchSeveralStatesInOneCall {
      () =>
        case class DeferredState(id: Int) extends Deferred[Option[TestApp.State]]

        val deferredResolver = new DeferredResolver[TestApp.Context] {
          override def resolve(
              deferred: Vector[Deferred[Any]],
              context: TestApp.Context,
              queryState: Any)(implicit ec: ExecutionContext): Vector[Future[Any]] = {
            val ids = deferred.collect { case DeferredState(id) => id }
            val states = context.db.statesByIds(ids)
            ids.map(id => states.map(_.find(_.id == id)))
          }
        }

        case class StateArg(id: Int)
        implicit val stateArgDecoder: Decoder[Json, StateArg] =
          deriveDecoder[StateArg].decodeJson(_)
        val stateResolver = EntityResolver[TestApp.Context, Json, TestApp.State, StateArg](
          __typeName = TestApp.StateType.name,
          (arg, _) => DeferredValue(DeferredState(arg.id))
        )

        (stateResolver, deferredResolver)
    }

    "fetch several entities of same type in one call using Fetcher" in FetchSeveralStatesInOneCall {
      () =>
        val states = Fetcher { (ctx: TestApp.Context, ids: Seq[Int]) =>
          ctx.db.statesByIds(ids)
        }

        case class StateArg(id: Int)
        implicit val stateArgDecoder: Decoder[Json, StateArg] =
          deriveDecoder[StateArg].decodeJson(_)
        val stateResolver = EntityResolver[TestApp.Context, Json, TestApp.State, StateArg](
          __typeName = TestApp.StateType.name,
          (arg, _) => states.deferOpt(arg.id)
        )

        (stateResolver, DeferredResolver.fetchers(states))
    }

    "fetch several entities of different types in one call" in {
      val testApp = AppWithResolvers()

      val args: Json = parse("""
          {
            "representations": [
              { "__typename": "State", "id": 1 },
              { "__typename": "State", "id": 2 },
              { "__typename": "Review", "id": 2 },
              { "__typename": "State", "id": 20 },
              { "__typename": "State", "id": 5 },
              { "__typename": "Review", "id": 1 }
            ]
          }
        """).getOrElse(Json.Null)

      val result = testApp.execute(fetchStateAndReview, args)

      QueryRenderer.renderPretty(result) should be("""{
         |  data: {
         |    _entities: [{
         |      id: 1
         |      value: "mock state 1"
         |    }, {
         |      id: 2
         |      value: "mock state 2"
         |    }, {
         |      id: 2
         |      value: "mock review 2"
         |    }, {
         |      id: 20
         |      value: "mock state 20"
         |    }, {
         |      id: 5
         |      value: "mock state 5"
         |    }, {
         |      id: 1
         |      value: "mock review 1"
         |    }]
         |  }
         |}""".stripMargin)

      testApp.testApp.db.stateDbCalled.get() should be(1)
      testApp.testApp.db.reviewDbCalled.get() should be(1)
    }

    "handles non found entities" in {
      val testApp = AppWithResolvers()

      val args: Json = parse(s"""
          {
            "representations": [
              { "__typename": "State", "id": 1 },
              { "__typename": "State", "id": ${TestApp.missingStateId} },
              { "__typename": "State", "id": 20 }
            ]
          }
        """).getOrElse(Json.Null)

      val result = testApp.execute(fetchStateAndReview, args)

      QueryRenderer.renderPretty(result) should be("""{
         |  data: {
         |    _entities: [{
         |      id: 1
         |      value: "mock state 1"
         |    }, null, {
         |      id: 20
         |      value: "mock state 20"
         |    }]
         |  }
         |}""".stripMargin)

      testApp.testApp.db.stateDbCalled.get() should be(1)
      testApp.testApp.db.reviewDbCalled.get() should be(0)
    }

    "handles entities using same arg" in {
      val testApp = new TestApp()

      val states = Fetcher { (ctx: TestApp.Context, ids: Seq[Int]) =>
        ctx.db.statesByIds(ids)
      }

      case class IntArg(id: Int)
      implicit val intArgDecoder: Decoder[Json, IntArg] =
        deriveDecoder[IntArg].decodeJson(_)
      val stateResolver = EntityResolver[TestApp.Context, Json, TestApp.State, IntArg](
        __typeName = TestApp.StateType.name,
        (arg, _) => states.deferOpt(arg.id)
      )

      val reviews = Fetcher { (ctx: TestApp.Context, ids: Seq[Int]) =>
        ctx.db.reviewsByIds(ids)
      }

      val reviewResolver = EntityResolver[TestApp.Context, Json, TestApp.Review, IntArg](
        __typeName = TestApp.ReviewType.name,
        (arg, _) => reviews.deferOpt(arg.id)
      )

      val schema: Schema[TestApp.Context, Any] =
        Federation.extend(TestApp.schema, List(stateResolver, reviewResolver))

      implicit val um = Federation.upgrade(sangria.marshalling.circe.CirceInputUnmarshaller)
      val variables: Json = parse("""
        {
          "representations": [
            { "__typename": "Review", "id": 1 },
            { "__typename": "State", "id": 1 },
            { "__typename": "State", "id": 2 }
          ]
        }
      """).getOrElse(Json.Null)

      import ExecutionContext.Implicits.global
      val result = await(
        Executor.execute(
          schema,
          fetchStateAndReview,
          userContext = testApp.ctx,
          variables = variables,
          deferredResolver = DeferredResolver.fetchers(states, reviews)))

      QueryRenderer.renderPretty(result) should be("""{
         |  data: {
         |    _entities: [{
         |      id: 1
         |      value: "mock review 1"
         |    }, {
         |      id: 1
         |      value: "mock state 1"
         |    }, {
         |      id: 2
         |      value: "mock state 2"
         |    }]
         |  }
         |}""".stripMargin)
    }
  }
}

object ResolverSpec {

  val Success(fetchStateAndReview) = QueryParser.parse("""
     query FetchState($representations: [_Any!]!) {
       _entities(representations: $representations) {
         ... on State {
           id
           value
         }
         ... on Review {
           id
           value
         }
       }
     }
     """)

  case class FetchSeveralStatesInOneCall(
      entityAndDeferredResolverF: () => (
          EntityResolver[TestApp.Context, Json],
          DeferredResolver[TestApp.Context]))
      extends Matchers {
    val testApp = new TestApp()
    val (stateResolver, deferredResolver) = entityAndDeferredResolverF()

    val schema: Schema[TestApp.Context, Any] =
      Federation.extend(TestApp.schema, List(stateResolver))

    val Success(query) = QueryParser.parse("""
       query FetchState($representations: [_Any!]!) {
         _entities(representations: $representations) {
           ... on State { id, value }
         }
       }
       """)

    val args: Json = parse("""
        {
          "representations": [
            { "__typename": "State", "id": 1 },
            { "__typename": "State", "id": 2 },
            { "__typename": "State", "id": 20 },
            { "__typename": "State", "id": 5 }
          ]
        }
      """).getOrElse(Json.Null)

    implicit val um: InputUnmarshaller[Json] =
      Federation.upgrade(sangria.marshalling.circe.CirceInputUnmarshaller)
    import ExecutionContext.Implicits.global
    val result: ast.Value = await(
      Executor.execute(
        schema,
        query,
        userContext = testApp.ctx,
        variables = args,
        deferredResolver = deferredResolver))

    QueryRenderer.renderPretty(result) should be("""{
       |  data: {
       |    _entities: [{
       |      id: 1
       |      value: "mock state 1"
       |    }, {
       |      id: 2
       |      value: "mock state 2"
       |    }, {
       |      id: 20
       |      value: "mock state 20"
       |    }, {
       |      id: 5
       |      value: "mock state 5"
       |    }]
       |  }
       |}""".stripMargin)
    testApp.db.stateDbCalled.get() should be(1)
  }

  case class AppWithResolvers() extends Matchers {
    val testApp = new TestApp()

    val states = Fetcher { (ctx: TestApp.Context, ids: Seq[Int]) =>
      ctx.db.statesByIds(ids)
    }

    case class StateArg(id: Int)
    implicit val stateArgDecoder: Decoder[Json, StateArg] =
      deriveDecoder[StateArg].decodeJson(_)
    val stateResolver = EntityResolver[TestApp.Context, Json, TestApp.State, StateArg](
      __typeName = TestApp.StateType.name,
      (arg, _) => states.deferOpt(arg.id)
    )

    val reviews = Fetcher { (ctx: TestApp.Context, ids: Seq[Int]) =>
      ctx.db.reviewsByIds(ids)
    }

    case class ReviewArg(id: Int)
    implicit val reviewArgDecoder: Decoder[Json, ReviewArg] =
      deriveDecoder[ReviewArg].decodeJson(_)
    val reviewResolver = EntityResolver[TestApp.Context, Json, TestApp.Review, ReviewArg](
      __typeName = TestApp.ReviewType.name,
      (arg, _) => reviews.deferOpt(arg.id)
    )

    val schema: Schema[TestApp.Context, Any] =
      Federation.extend(TestApp.schema, List(stateResolver, reviewResolver))

    implicit val um: InputUnmarshaller[Json] =
      Federation.upgrade(sangria.marshalling.circe.CirceInputUnmarshaller)

    def execute[Input](query: ast.Document, variables: Json): ast.Value = {
      import ExecutionContext.Implicits.global
      await(
        Executor.execute(
          schema,
          query,
          userContext = testApp.ctx,
          variables = variables,
          deferredResolver = DeferredResolver.fetchers(states, reviews)))
    }

  }
}
