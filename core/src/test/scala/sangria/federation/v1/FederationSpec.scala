package sangria.federation.v1

import scala.util.Success
import io.circe.Json
import io.circe.generic.semiauto._
import io.circe.parser._
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers._
import sangria.ast.Document
import sangria.execution.{Executor, VariableCoercionError}
import sangria.federation._
import sangria.federation.v1.Directives.Key
import sangria.macros.LiteralGraphQLStringContext
import sangria.parser.QueryParser
import sangria.renderer.QueryRenderer
import sangria.schema._

class FederationSpec extends AsyncFreeSpec {

  "federation schema v1" - {
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
            schema {
              query: Query
            }

            type Query {
              field: Int
              _service: _Service!
            }

            scalar _FieldSet

            scalar _Any

            type _Service {
              sdl: String
            }

            directive @extends on INTERFACE | OBJECT

            directive @external on FIELD_DEFINITION

            directive @requires(fields: _FieldSet!) on FIELD_DEFINITION

            directive @provides(fields: _FieldSet!) on FIELD_DEFINITION

            directive @key(fields: _FieldSet!) repeatable on OBJECT | INTERFACE
          """)
          .extend(Document(Vector(_FieldSet.Type.toAst)))

        schema should beCompatibleWith(expectedSubGraphSchema)
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
            schema {
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

            type _Service {
              sdl: String
            }

            directive @extends on INTERFACE | OBJECT

            directive @external on FIELD_DEFINITION

            directive @requires(fields: _FieldSet!) on FIELD_DEFINITION

            directive @provides(fields: _FieldSet!) on FIELD_DEFINITION

            directive @key(fields: _FieldSet!) repeatable on OBJECT | INTERFACE
          """)
          .extend(Document(Vector(_FieldSet.Type.toAst)))

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
                |      sdl: "type Query {\n  field: Int\n}"
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
                |      sdl: "type Query {\n  states: [State]\n}\n\ntype State @key(fields: \"id\") {\n  id: Int\n  value: String\n}"
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
                |      sdl: "\"The `Long` scalar type represents non-fractional signed whole numeric values. Long can represent values between -(2^63) and 2^63 - 1.\"\nscalar Long\n\ntype Query {\n  foo: Long\n  bar: Int\n}"
                |    }
                |  }
                |}""".stripMargin))
      }
    }

    "Apollo gateway queries" - {

      val Success(query) = QueryParser.parse("""
         query FetchState($representations: [_Any!]!) {
           _entities(representations: $representations) {
             ... on State { id value }
             ... on Review { id value }
             ... on Review2 { id value2 }
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

      "handles entities using same arg" in {
        implicit val um = Federation.upgrade(sangria.marshalling.circe.CirceInputUnmarshaller)
        val args: Json = parse("""
          {
            "representations": [
              { "__typename": "Review", "id": 1 },
              { "__typename": "Review2", "id": 1 },
              { "__typename": "Review2", "id": 2 }
            ]
          }
        """).getOrElse(Json.Null)

        Executor
          .execute(FederationSpec.Schema.schema, query, variables = args)
          .map(QueryRenderer.renderPretty(_) should be("""{
              |  data: {
              |    _entities: [{
              |      id: 1
              |      value: "mock review 1"
              |    }, {
              |      id: 1
              |      value2: "mock review2 1"
              |    }, {
              |      id: 2
              |      value2: "mock review2 2"
              |    }]
              |  }
              |}""".stripMargin))
      }
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
      __typeName = StateType.name,
      (args, _) => args.filterNot(_.id == 42).map(arg => State(arg.id, s"mock state ${arg.id}")),
      s => StateArg(s.id))

    private case class Review(id: Int, value: String)
    private val ReviewType = ObjectType(
      "Review",
      fields[Unit, Review](
        Field("id", IntType, resolve = _.value.id),
        Field("value", OptionType(StringType), resolve = _.value.value))).withDirective(Key("id"))

    private case class ReviewArg(id: Int)
    private implicit val reviewArgDecoder: Decoder[Json, ReviewArg] =
      deriveDecoder[ReviewArg].decodeJson(_)
    private val reviewResolver = EntityResolver[Any, Json, Review, ReviewArg](
      __typeName = ReviewType.name,
      (args, _) => args.map(arg => Review(arg.id, s"mock review ${arg.id}")),
      r => ReviewArg(r.id))

    private case class Review2(id: Int, value2: String)

    private val Review2Type = ObjectType(
      "Review2",
      fields[Unit, Review2](
        Field("id", IntType, resolve = _.value.id),
        Field("value2", OptionType(StringType), resolve = _.value.value2))).withDirective(Key("id"))
    // review2 uses the same arg as review
    private val review2Resolver = EntityResolver[Any, Json, Review2, ReviewArg](
      __typeName = Review2Type.name,
      (args, _) => args.map(arg => Review2(arg.id, s"mock review2 ${arg.id}")),
      r => ReviewArg(r.id))

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
      stateResolver :: reviewResolver :: review2Resolver :: Nil
    )
  }
}
