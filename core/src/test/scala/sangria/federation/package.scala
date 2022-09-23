package sangria

import sangria.schema.Schema
import sangria.schema.SchemaChange.AbstractChange
import org.scalatest.matchers.{MatchResult, Matcher}

package object federation {

  def beCompatibleWith(expectedSchema: Schema[_, _]): Matcher[Schema[_, _]] =
    Matcher { schema: Schema[_, _] =>
      val changes = schema.compare(expectedSchema)

      MatchResult(
        changes.collect { case _: AbstractChange => true }.isEmpty,
        s"Schemas have following changes:\n  ${changes.mkString("\n  ")}",
        s"Schemas should be different but no changes can be found"
      )
    }
}
