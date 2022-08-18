package sangria

import sangria.schema.Schema
import sangria.schema.SchemaChange.AbstractChange
import org.scalatest.matchers.{MatchResult, Matcher}

package object federation {

  def beCompatibleWith(expectedSchema: Schema[_, _]) =
    Matcher { schema: Schema[_, _] =>
      val changes = schema.compare(expectedSchema).collect { case _: AbstractChange => true }
      MatchResult(
        changes.isEmpty,
        s"Schemas have following changes: ${changes.mkString(", ")}",
        s"Schemas should be different but no changes can be found"
      )
    }
}
