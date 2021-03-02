package sangria.federation

import sangria.schema.StringType

case class _FieldSet(fields: String)

object _FieldSet {

  val Type = StringType.rename("_FieldSet").copy(description = None)
}
