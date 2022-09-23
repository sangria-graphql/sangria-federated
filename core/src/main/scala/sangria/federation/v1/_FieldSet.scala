package sangria.federation.v1

import sangria.schema.{ScalarType, StringType}

case class _FieldSet(fields: String)

object _FieldSet {

  val Type: ScalarType[String] = StringType.rename("_FieldSet").copy(description = None)
}
