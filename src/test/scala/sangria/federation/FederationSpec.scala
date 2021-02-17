package sangria.federation

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Success

import io.circe.Json
import io.circe.generic.semiauto._
import io.circe.parser._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers._
import sangria.execution.Executor
import sangria.macros.LiteralGraphQLStringContext
import sangria.parser.QueryParser
import sangria.schema._
import sangria.schema.SchemaChange.DirectiveRemoved

class FederationSpec extends AnyFreeSpec {

  "federation schema" - {
    "should include the following directives" in {
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

      val otherSchema = Schema.buildFromAst(graphql"""
          schema {
            query: Query
          }

          type Query {
            field: Int
          }

          directive @extends on INTERFACE | OBJECT

          directive @external on FIELD_DEFINITION

          directive @key(fields: String!) on INTERFACE | OBJECT

          directive @provides on FIELD_DEFINITION

          directive @requires on FIELD_DEFINITION
        """)

      schema.compare(otherSchema).collect({ case _: DirectiveRemoved => true }) shouldBe empty
    }

    "should be able to answer Apollo gateway queries" in {

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

      import scala.concurrent.ExecutionContext.Implicits.global
      import sangria.marshalling.queryAst.queryAstResultMarshaller

      implicit val um = Federation.upgrade(sangria.marshalling.circe.CirceInputUnmarshaller)

      val result = Await.result(
        Executor
          .execute(schema, query, variables = args),
        10.seconds)

      result.renderPretty should be("""{
        |  data: {
        |    _entities: [{
        |      id: 1
        |      value: "mock"
        |    }]
        |  }
        |}""".stripMargin)
    }
  }
}
