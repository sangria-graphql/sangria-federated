package federation

import sangria.schema.{ScalarAlias, StringType}

case class _FieldSet(fields: String)

object _FieldSet {

  val Type = ScalarAlias[_FieldSet, String](StringType, _.fields, str => Right(_FieldSet(str)))
}
