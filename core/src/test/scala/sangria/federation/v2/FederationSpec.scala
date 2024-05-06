package sangria.federation.v2

import io.circe.Json
import io.circe.generic.semiauto._
import io.circe.parser._
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers._
import sangria.execution.deferred.{DeferredResolver, Fetcher, HasId}
import sangria.execution.{Executor, VariableCoercionError}
import sangria.federation._
import sangria.federation.v2.Directives.Key
import sangria.macros.LiteralGraphQLStringContext
import sangria.parser.QueryParser
import sangria.renderer.{QueryRenderer, SchemaRenderer}
import sangria.schema._

import scala.concurrent.Future
import scala.util.Success

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
           }
         }
         """)

      import sangria.marshalling.queryAst.queryAstResultMarshaller

      "should succeed on federated unmarshaller" in {
        implicit val um = Federation.upgrade(sangria.marshalling.circe.CirceInputUnmarshaller)
        val args: Json = parse(""" { "representations": [{ "__typename": "State", "id": 1 }] } """)
          .getOrElse(Json.Null)

        Executor
          .execute(
            FederationSpec.Schema.schema,
            query,
            variables = args,
            deferredResolver = DeferredResolver.fetchers(FederationSpec.Schema.states))
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
    }
  }
}

object FederationSpec {
  object Schema {
    // =================== State ===================
    // State model
    case class State(id: Int, value: String)

    // State GraphQL Model
    private val StateType = ObjectType(
      "State",
      fields[Unit, State](
        Field("id", IntType, resolve = _.value.id),
        Field("value", OptionType(StringType), resolve = _.value.value))).withDirective(Key("id"))

    // State fetcher
    private implicit val stateId: HasId[State, Int] = _.id
    val states: Fetcher[Any, State, State, Int] = Fetcher { (_: Any, ids: Seq[Int]) =>
      Future.successful(ids.map(id => State(id, s"mock state $id")))
    }

    // State resolver
    private case class StateArg(id: Int)
    private implicit val stateArgDecoder: Decoder[Json, StateArg] =
      deriveDecoder[StateArg].decodeJson(_)
    private val stateResolver = EntityResolver[Any, Json, State, StateArg](
      __typeName = StateType.name,
      (arg, _) => states.deferOpt(arg.id))

    // =================== Query ===================
    private val Query = ObjectType(
      "Query",
      fields[Unit, Any](
        Field(name = "states", fieldType = ListType(StateType), resolve = _ => Nil)
      )
    )

    // =================== Schema ===================
    val schema: Schema[Any, Any] = Federation.extend(
      sangria.schema.Schema(Query),
      List(stateResolver)
    )
  }
}
