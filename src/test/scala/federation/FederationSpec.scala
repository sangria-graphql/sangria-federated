package federation

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers._
import sangria.macros.LiteralGraphQLStringContext
import sangria.schema.Schema
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

      schema.compare(otherSchema).filter(_.isInstanceOf[DirectiveRemoved]) shouldBe empty
    }
  }
}
