package sangria

import org.scalatest.matchers.must.Matchers.be
import org.scalatest.matchers.{MatchResult, Matcher}
import sangria.renderer.QueryRenderer
import sangria.schema.Schema
import sangria.schema.SchemaChange.AbstractChange

package object federation {

  val spec: String = "https://specs.apollo.dev/federation/v2.1"

  def beCompatibleWith(expectedSchema: Schema[_, _]): Matcher[Schema[_, _]] =
    Matcher { schema: Schema[_, _] =>
      val changes = schema.compare(expectedSchema)

      MatchResult(
        changes.collect { case _: AbstractChange => true }.isEmpty,
        s"Schemas have following changes:\n  ${changes.mkString("\n  ")}",
        s"Schemas should be different but no changes can be found"
      )
    }

  private def pretty(v: Vector[_]): String = v.mkString("\n - ", "\n - ", "")

  def importFederationDirective(name: String): Matcher[Schema[_, _]] =
    Matcher { schema: Schema[_, _] =>
      val links = schema.astDirectives.filter(d => d.name == "link")
      val link = links.find(d =>
        d.arguments.exists(arg => arg.name == "url" && arg.value == ast.StringValue(spec)))

      val error = link match {
        case None => Some(s"""no link with url "$spec" found under all links:${pretty(links)}""")
        case Some(federationLink) =>
          federationLink.arguments.find(_.name == "import") match {
            case None => Some(s"""the "$spec" link does not have any "imports""")
            case Some(importValue) =>
              importValue.value match {
                case lv: ast.ListValue =>
                  lv.values.find {
                    case sv: ast.StringValue if sv.value == name => true
                    case _ => false
                  } match {
                    case None =>
                      Some(
                        s"""the "import" value of the "$spec" link does not contain "$name" but contain:${pretty(
                            lv.values)}""")
                    case _ => None
                  }
                case other =>
                  Some(
                    s"""the "$spec" link does not have an array value for  "imports" but "${other.getClass}"""")
              }
          }
      }
      MatchResult(
        error.isEmpty,
        error.getOrElse(""),
        error.getOrElse("")
      )
    }

  def renderLike(expected: String): Matcher[ast.Directive] =
    Matcher { directive: ast.Directive =>
      val rendered = QueryRenderer.renderPretty(directive)
      be(expected)(rendered)
    }
}
