package sangria.federation.v2

import org.scalatest.matchers.should.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import sangria.ast
import sangria.federation.importFederationDirective
import sangria.federation.v2.ComposeDirectiveSpec.{helloDirective, initialSchema, myDirective}
import sangria.federation.v2.Directives.{ComposeDirective, Link}
import sangria.macros.LiteralGraphQLStringContext
import sangria.renderer.SchemaRenderer
import sangria.schema._
import sangria.util.tag.@@

object ComposeDirectiveSpec {
  val initialSchema = Schema.buildFromAst(graphql"""
    schema { query: Query }
    type Query { field: Int }
  """)

  val myDirective: Directive =
    Directive("myDirective", locations = Set(DirectiveLocation.FieldDefinition))

  val helloDirective: Directive =
    Directive("hello", locations = Set(DirectiveLocation.FieldDefinition))
}

class ComposeDirectiveSpec extends AnyWordSpec {
  "@composeDirective" should {
    "import directives, using the high level API" in {
      val schema = Federation.extend(
        schema = initialSchema,
        customDirectives = CustomDirectivesDefinition(
          Spec("https://myspecs.dev/myDirective/v1.0") -> List(myDirective)),
        resolvers = Nil
      )

      schema should importFederationDirective("@composeDirective")

      val renderedSchema = SchemaRenderer.renderSchema(schema)
      renderedSchema should include(
        """@link(url: "https://myspecs.dev/myDirective/v1.0", import: ["@myDirective"])""")
      renderedSchema should include("""@composeDirective(name: "@myDirective")""")
      renderedSchema should include("""directive @myDirective on FIELD_DEFINITION""")
    }

    "import directives in multiple specs, using the high level API" in {
      val schema = Federation.extend(
        schema = initialSchema,
        customDirectives = CustomDirectivesDefinition(
          Spec("https://myspecs.dev/myDirective/v1.0") -> List(myDirective),
          Spec("https://myspecs.dev/helloDirective/v2.0") -> List(helloDirective)
        ),
        resolvers = Nil
      )

      schema should importFederationDirective("@composeDirective")

      val renderedSchema = SchemaRenderer.renderSchema(schema)
      renderedSchema should include(
        """@link(url: "https://myspecs.dev/myDirective/v1.0", import: ["@myDirective"])""")
      renderedSchema should include(
        """@link(url: "https://myspecs.dev/helloDirective/v2.0", import: ["@hello"])""")
      renderedSchema should include("""@composeDirective(name: "@myDirective")""")
      renderedSchema should include("""@composeDirective(name: "@hello")""")
      renderedSchema should include("""directive @myDirective on FIELD_DEFINITION""")
      renderedSchema should include("""directive @hello on FIELD_DEFINITION""")
    }

    "import directives with alias, using the low level API" in {
      val additionalLinkImports: List[ast.Directive @@ Link] = List(
        Link(
          url = "https://myspecs.dev/myDirective/v1.0",
          `import` = Some(
            Vector(
              Link__Import("@" + myDirective.name),
              Link__Import("@anotherDirective").as("@hello")
            ))))

      val composeDirectives: List[ast.Directive @@ ComposeDirective] = List(
        Directives.ComposeDirective(myDirective),
        Directives.ComposeDirective(helloDirective)
      )

      val schemaWithDirectives = initialSchema.copy(
        directives = myDirective :: helloDirective :: initialSchema.directives
      )

      val schema = Federation.extend(
        schema = schemaWithDirectives,
        additionalLinkImports = additionalLinkImports,
        composeDirectives = composeDirectives,
        resolvers = Nil
      )

      schema should importFederationDirective("@composeDirective")

      val renderedSchema = SchemaRenderer.renderSchema(schema)
      renderedSchema should include(
        """@link(url: "https://myspecs.dev/myDirective/v1.0", import: ["@myDirective", {name: "@anotherDirective", as: "@hello"}])""")
      renderedSchema should include("""@composeDirective(name: "@myDirective")""")
      renderedSchema should include("""@composeDirective(name: "@hello")""")
      renderedSchema should include("""directive @myDirective on FIELD_DEFINITION""")
      renderedSchema should include("""directive @hello on FIELD_DEFINITION""")
    }
  }
}
