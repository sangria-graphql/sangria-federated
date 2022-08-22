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
import sangria.macros.LiteralGraphQLStringContext
import sangria.parser.QueryParser
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

            directive @key(fields: _FieldSet!) on OBJECT | INTERFACE
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
            }

            type State @key(fields: "id") {
              id: Int
              value: String
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
              _entities(representations: [_Any!]!): [_Entity]!
              _service: _Service!
            }

            type State @key(fields: "id") {
              id: Int
              value: String
            }

            union _Entity = State

            scalar _FieldSet

            scalar _Any

            type _Service {
              sdl: String
            }

            directive @extends on INTERFACE | OBJECT

            directive @external on FIELD_DEFINITION

            directive @requires(fields: _FieldSet!) on FIELD_DEFINITION

            directive @provides(fields: _FieldSet!) on FIELD_DEFINITION

            directive @key(fields: _FieldSet!) on OBJECT | INTERFACE
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
            .map(_.renderPretty should be("""{
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
            .map(_.renderPretty should be("""{
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
          .map(_.renderPretty should be("""{
                |  data: {
                |    _service: {
                |      sdl: "\"The `Long` scalar type represents non-fractional signed whole numeric values. Long can represent values between -(2^63) and 2^63 - 1.\"\nscalar Long\n\ntype Query {\n  foo: Long\n  bar: Int\n}"
                |    }
                |  }
                |}""".stripMargin))
      }
    }

    "Apollo gateway queries" - {

      case class State(id: Int, value: String)
      case class StateArg(id: Int)

      implicit val decoder: Decoder[Json, StateArg] = deriveDecoder[StateArg].decodeJson(_)

      val stateResolver = EntityResolver[Any, Json, State, StateArg](
        __typeName = "State",
        arg => Some(State(arg.id, "mock")))

      val schema = Federation.extend(
        Schema.buildFromAst(
          graphql"""
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
           """,
          AstSchemaBuilder.resolverBased[Any](
            FieldResolver.map(
              "Query" -> Map(
                "states" -> (ctx => Nil)
              )
            ),
            FieldResolver.map(
              "State" -> Map(
                "id" -> (ctx => ctx.value.asInstanceOf[State].id),
                "value" -> (ctx => ctx.value.asInstanceOf[State].value)
              ))
          )
        ),
        stateResolver :: Nil
      )

      val Success(query) = QueryParser.parse("""
         query FetchState($representations: [_Any!]!) {
           _entities(representations: $representations) {
             ... on State {
               id
               value
             }
           }
         }
         """)

      val args: Json = parse(""" { "representations": [{ "__typename": "State", "id": 1 }] } """)
        .getOrElse(Json.Null)

      import sangria.marshalling.queryAst.queryAstResultMarshaller

      "should succeed on federated unmarshaller" in {

        implicit val um = Federation.upgrade(sangria.marshalling.circe.CirceInputUnmarshaller)

        Executor
          .execute(schema, query, variables = args)
          .map(_.renderPretty should be("""{
              |  data: {
              |    _entities: [{
              |      id: 1
              |      value: "mock"
              |    }]
              |  }
              |}""".stripMargin))
      }

      "should fail on regular unmarshaller" in {

        implicit val um = sangria.marshalling.circe.CirceInputUnmarshaller

        recoverToSucceededIf[VariableCoercionError] {
          Executor
            .execute(schema, query, variables = args)
        }
      }
    }
  }
}
