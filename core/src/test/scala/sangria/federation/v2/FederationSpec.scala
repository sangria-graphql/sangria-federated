package sangria.federation.v2

import scala.util.Success
import io.circe.Json
import io.circe.generic.semiauto._
import io.circe.parser._
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers._
import sangria.ast.Document
import sangria.execution.deferred.{Deferred, DeferredResolver, UnsupportedDeferError}
import sangria.execution.{Executor, VariableCoercionError}
import sangria.federation._
import sangria.federation.v2.Directives.Key
import sangria.macros.LiteralGraphQLStringContext
import sangria.parser.QueryParser
import sangria.renderer.{QueryRenderer, SchemaRenderer}
import sangria.schema._

import scala.concurrent.{ExecutionContext, Future}

class FederationSpec extends AsyncFreeSpec {

  "federation schema v2" - {
    "should respect Apollo specification" - {
      "in case no entity is defined" in {
        val schema = Federation.extend(
          Schema.buildFromAst(graphql"""
            schema {
              query: Query
            }

            type Query {
              field: Int
            }
          """),
          Nil
        )

        val expectedSubGraphSchema = Schema
          .buildFromAst(graphql"""
            schema @link(url: "https://specs.apollo.dev/federation/v2.3", import: ["@key", "@interfaceObject", "@extends", "@shareable", "@inaccessible", "@override", "@external", "@provides", "@requires", "@tag"]) {
              query: Query
            }

            type Query {
              field: Int
              _service: _Service!
            }

            scalar _FieldSet

            scalar _Any

            scalar link__Import

            enum link__Purpose {
              "`SECURITY` features provide metadata necessary to securely resolve fields."
              SECURITY

              "`EXECUTION` features provide metadata necessary for operation execution."
              EXECUTION
            }

            type _Service {
              sdl: String
            }

            directive @interfaceObject on OBJECT

            directive @extends on INTERFACE | OBJECT

            directive @external on FIELD_DEFINITION

            directive @requires(fields: _FieldSet!) on FIELD_DEFINITION

            directive @provides(fields: _FieldSet!) on FIELD_DEFINITION

            directive @key(fields: _FieldSet!, resolvable: Boolean = true) repeatable on OBJECT | INTERFACE

            directive @link(url: String!, as: String, for: link__Purpose, import: [link__Import]) repeatable on SCHEMA

            directive @shareable repeatable on OBJECT | FIELD_DEFINITION

            directive @inaccessible on FIELD_DEFINITION | OBJECT | INTERFACE | UNION | ARGUMENT_DEFINITION | SCALAR | ENUM | ENUM_VALUE | INPUT_OBJECT | INPUT_FIELD_DEFINITION

            directive @override(from: String!) on FIELD_DEFINITION

            directive @tag(name: String!) repeatable on FIELD_DEFINITION | INTERFACE | OBJECT | UNION | ARGUMENT_DEFINITION | SCALAR | ENUM | ENUM_VALUE | INPUT_OBJECT | INPUT_FIELD_DEFINITION
          """)

        withClue(SchemaRenderer.renderSchema(schema)) {
          schema should beCompatibleWith(expectedSubGraphSchema)
        }
      }

      "in case entities are defined" in {
        val schema = Federation.extend(
          Schema.buildFromAst(graphql"""
            schema {
              query: Query
            }

            type Query {
              states: [State]
              reviews: [Review]
            }

            type State @key(fields: "id") {
              id: Int
              value: String
            }

            type Review @key(fields: "id") {
              id: Int
              comment: String
            }
          """),
          Nil
        )

        val expectedSubGraphSchema = Schema
          .buildFromAst(graphql"""
            schema @link(url: "https://specs.apollo.dev/federation/v2.3", import: ["@key", "@interfaceObject", "@extends", "@shareable", "@inaccessible", "@override", "@external", "@provides", "@requires", "@tag"]) {
              query: Query
            }

            type Query {
              states: [State]
              reviews: [Review]
              _entities(representations: [_Any!]!): [_Entity]!
              _service: _Service!
            }

            type State @key(fields: "id") {
              id: Int
              value: String
            }

            type Review @key(fields: "id") {
              id: Int
              comment: String
            }
            union _Entity = State | Review

            scalar _FieldSet

            scalar _Any

            scalar link__Import

            enum link__Purpose {
              "`SECURITY` features provide metadata necessary to securely resolve fields."
              SECURITY

              "`EXECUTION` features provide metadata necessary for operation execution."
              EXECUTION
            }

            type _Service {
              sdl: String
            }

            directive @interfaceObject on OBJECT

            directive @extends on INTERFACE | OBJECT

            directive @external on FIELD_DEFINITION

            directive @requires(fields: _FieldSet!) on FIELD_DEFINITION

            directive @provides(fields: _FieldSet!) on FIELD_DEFINITION

            directive @key(fields: _FieldSet!, resolvable: Boolean = true) repeatable on OBJECT | INTERFACE

            directive @link(url: String!, as: String, for: link__Purpose, import: [link__Import]) repeatable on SCHEMA

            directive @shareable repeatable on OBJECT | FIELD_DEFINITION

            directive @inaccessible on FIELD_DEFINITION | OBJECT | INTERFACE | UNION | ARGUMENT_DEFINITION | SCALAR | ENUM | ENUM_VALUE | INPUT_OBJECT | INPUT_FIELD_DEFINITION

            directive @override(from: String!) on FIELD_DEFINITION

            directive @tag(name: String!) repeatable on FIELD_DEFINITION | INTERFACE | OBJECT | UNION | ARGUMENT_DEFINITION | SCALAR | ENUM | ENUM_VALUE | INPUT_OBJECT | INPUT_FIELD_DEFINITION
          """)

        schema should beCompatibleWith(expectedSubGraphSchema)
      }
    }

    "_service sdl field" - {
      import sangria.marshalling.queryAst.queryAstResultMarshaller

      val Success(query) = QueryParser.parse("""
            query {
              _service {
                sdl
              }
            }
          """)

      "should not include federation types" - {
        "in case no entity is defined" in {
          val schema = Federation.extend(
            Schema.buildFromAst(graphql"""
                schema {
                  query: Query
                }

                type Query {
                  field: Int
                }
              """),
            Nil
          )

          Executor
            .execute(schema, query)
            .map(QueryRenderer.renderPretty(_) should be("""{
                |  data: {
                |    _service: {
                |      sdl: "schema @link(url: \"https://specs.apollo.dev/federation/v2.3\", import: [\"@key\", \"@interfaceObject\", \"@extends\", \"@shareable\", \"@inaccessible\", \"@override\", \"@external\", \"@provides\", \"@requires\", \"@tag\"]) {\n  query: Query\n}\n\ntype Query {\n  field: Int\n}"
                |    }
                |  }
                |}""".stripMargin))
        }

        "in case entities are defined" in {
          val schema = Federation.extend(
            Schema.buildFromAst(graphql"""
              schema {
                query: Query
              }

              type Query {
                states: [State]
              }

              type State @key(fields: "id") {
                id: Int
                value: String
              }
            """),
            Nil
          )

          Executor
            .execute(schema, query)
            .map(QueryRenderer.renderPretty(_) should be("""{
                |  data: {
                |    _service: {
                |      sdl: "schema @link(url: \"https://specs.apollo.dev/federation/v2.3\", import: [\"@key\", \"@interfaceObject\", \"@extends\", \"@shareable\", \"@inaccessible\", \"@override\", \"@external\", \"@provides\", \"@requires\", \"@tag\"]) {\n  query: Query\n}\n\ntype Query {\n  states: [State]\n}\n\ntype State @key(fields: \"id\") {\n  id: Int\n  value: String\n}"
                |    }
                |  }
                |}""".stripMargin))
        }
      }

      "should not filter Sangria built-in types and filter GraphQL built-in types" in {
        val schema = Federation.extend(
          Schema.buildFromAst(graphql"""
                schema {
                  query: Query
                }

                type Query {
                  foo: Long
                  bar: Int
                }
              """),
          Nil
        )

        Executor
          .execute(schema, query)
          .map(QueryRenderer.renderPretty(_) should be("""{
                |  data: {
                |    _service: {
                |      sdl: "schema @link(url: \"https://specs.apollo.dev/federation/v2.3\", import: [\"@key\", \"@interfaceObject\", \"@extends\", \"@shareable\", \"@inaccessible\", \"@override\", \"@external\", \"@provides\", \"@requires\", \"@tag\"]) {\n  query: Query\n}\n\n\"The `Long` scalar type represents non-fractional signed whole numeric values. Long can represent values between -(2^63) and 2^63 - 1.\"\nscalar Long\n\ntype Query {\n  foo: Long\n  bar: Int\n}"
                |    }
                |  }
                |}""".stripMargin))
      }
    }

    "Apollo gateway queries" - {

      val Success(query) = QueryParser.parse("""
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

      import sangria.marshalling.queryAst.queryAstResultMarshaller

      "should succeed on federated unmarshaller" in {
        implicit val um = Federation.upgrade(sangria.marshalling.circe.CirceInputUnmarshaller)
        val args: Json = parse(""" { "representations": [{ "__typename": "State", "id": 1 }] } """)
          .getOrElse(Json.Null)

        Executor
          .execute(FederationSpec.Schema.schema, query, variables = args)
          .map(QueryRenderer.renderPretty(_) should be("""{
              |  data: {
              |    _entities: [{
              |      id: 1
              |      value: "mock state 1"
              |    }]
              |  }
              |}""".stripMargin))
      }

      "should fail on regular unmarshaller" in {
        implicit val um = sangria.marshalling.circe.CirceInputUnmarshaller
        val args: Json = parse(""" { "representations": [{ "__typename": "State", "id": 1 }] } """)
          .getOrElse(Json.Null)

        recoverToSucceededIf[VariableCoercionError] {
          Executor
            .execute(FederationSpec.Schema.schema, query, variables = args)
        }
      }

      "should fetch several entities of same type in one call" in {
        implicit val um = Federation.upgrade(sangria.marshalling.circe.CirceInputUnmarshaller)
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

        Executor
          .execute(FederationSpec.Schema.schema, query, variables = args)
          .map(QueryRenderer.renderPretty(_) should be("""{
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
             |}""".stripMargin))
      }

      "should fetch several entities of different types in one call" in {
        implicit val um = Federation.upgrade(sangria.marshalling.circe.CirceInputUnmarshaller)
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

        Executor
          .execute(
            FederationSpec.Schema.schema,
            query,
            variables = args,
            deferredResolver = FederationSpec.Schema.deferredReviewResolver)
          .map(QueryRenderer.renderPretty(_) should be("""{
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
             |}""".stripMargin))
      }

      "handles non found entities" in {
        implicit val um = Federation.upgrade(sangria.marshalling.circe.CirceInputUnmarshaller)
        val args: Json = parse("""
          {
            "representations": [
              { "__typename": "State", "id": 1 },
              { "__typename": "State", "id": 42 },
              { "__typename": "State", "id": 20 }
            ]
          }
        """).getOrElse(Json.Null)

        Executor
          .execute(FederationSpec.Schema.schema, query, variables = args)
          .map(QueryRenderer.renderPretty(_) should be("""{
             |  data: {
             |    _entities: [{
             |      id: 1
             |      value: "mock state 1"
             |    }, null, {
             |      id: 20
             |      value: "mock state 20"
             |    }]
             |  }
             |}""".stripMargin))
      }

//      "handles entities using same arg" in {
//        implicit val um = Federation.upgrade(sangria.marshalling.circe.CirceInputUnmarshaller)
//        val args: Json = parse("""
//          {
//            "representations": [
//              { "__typename": "Review", "id": 1 },
//              { "__typename": "Review2", "id": 1 },
//              { "__typename": "Review2", "id": 2 }
//            ]
//          }
//        """).getOrElse(Json.Null)
//
//        Executor
//          .execute(FederationSpec.Schema.schema, query, variables = args)
//          .map(QueryRenderer.renderPretty(_) should be("""{
//             |  data: {
//             |    _entities: [{
//             |      id: 1
//             |      value: "mock review 1"
//             |    }, {
//             |      id: 1
//             |      value2: "mock review2 1"
//             |    }, {
//             |      id: 2
//             |      value2: "mock review2 2"
//             |    }]
//             |  }
//             |}""".stripMargin))
//      }
    }
  }
}

object FederationSpec {
  object Schema {
    private case class State(id: Int, value: String)
    private val StateType = ObjectType(
      "State",
      fields[Unit, State](
        Field("id", IntType, resolve = _.value.id),
        Field("value", OptionType(StringType), resolve = _.value.value))).withDirective(Key("id"))

    private case class StateArg(id: Int)
    private implicit val stateArgDecoder: Decoder[Json, StateArg] =
      deriveDecoder[StateArg].decodeJson(_)
    private val stateResolver = EntityResolver[Any, Json, State, StateArg](
      __typeName = "State",
      (arg, _) => if (arg.id == 42) None else Some(State(arg.id, s"mock state ${arg.id}")))

    private case class Review(id: Int, value: String)
    private case class DeferredReview(id: Int) extends Deferred[Option[Review]]
    private case class DeferredReviewSeq(ids: Seq[Int]) extends Deferred[Seq[Review]]
    case class DeferredReviewResolver() extends DeferredResolver[Any] {
      override def resolve(deferred: Vector[Deferred[Any]], context: Any, queryState: Any)(implicit
          ec: ExecutionContext): Vector[Future[Any]] =
        deferred.map {
          case deferredReview: DeferredReview =>
            Future.successful(Review(deferredReview.id, s"mock review ${deferredReview.id}"))
          case deferredReviews: DeferredReviewSeq =>
            Future.successful(deferredReviews.ids.map(id => Review(id, s"mock review $id")))
          case d => Future.failed(UnsupportedDeferError(d))
        }
    }
    val deferredReviewResolver: DeferredReviewResolver = DeferredReviewResolver()
    private val ReviewType = ObjectType(
      "Review",
      fields[Unit, Review](
        Field("id", IntType, resolve = _.value.id),
        Field("value", OptionType(StringType), resolve = _.value.value))).withDirective(Key("id"))

    private case class ReviewArg(id: Int)
    private implicit val reviewArgDecoder: Decoder[Json, ReviewArg] =
      deriveDecoder[ReviewArg].decodeJson(_)
    private val reviewResolver = EntityResolver[Any, Json, Review, ReviewArg](
      __typeName = "Review",
      (arg, _) => DeferredValue(DeferredReview(arg.id))
    )

    private case class Review2(id: Int, value2: String)

    private val Review2Type = ObjectType(
      "Review2",
      fields[Unit, Review2](
        Field("id", IntType, resolve = _.value.id),
        Field("value2", OptionType(StringType), resolve = _.value.value2))).withDirective(Key("id"))
    // review2 uses the same arg as review
    private val review2Resolver = EntityResolver[Any, Json, Review2, ReviewArg](
      __typeName = Review2Type.name,
      (arg, _) => Some(Review2(arg.id, s"mock review2 ${arg.id}")))

    private val Query = ObjectType(
      "Query",
      fields[Unit, Any](
        Field(name = "states", fieldType = ListType(StateType), resolve = _ => Nil),
        Field(name = "reviews", fieldType = ListType(ReviewType), resolve = _ => Nil),
        Field(name = "reviews2", fieldType = ListType(Review2Type), resolve = _ => Nil)
      )
    )

    val schema: Schema[Any, Any] = Federation.extend(
      sangria.schema.Schema(Query),
      List(stateResolver, reviewResolver, review2Resolver)
    )
  }
}
