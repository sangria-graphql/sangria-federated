package sangria.federation

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers._
import sangria.ast.Document
import sangria.macros.LiteralGraphQLStringContext
import sangria.schema.Schema
import sangria.schema.SchemaChange.AbstractChange

class FederationSpec extends AnyFreeSpec {

  "federation schema" - {
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

        val otherSchema = Schema
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

        schema.compare(otherSchema).collect({ case _: AbstractChange => true }) shouldBe empty
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

        val otherSchema = Schema
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

        schema.compare(otherSchema).collect({ case _: AbstractChange => true }) shouldBe empty
      }
    }
  }
}
