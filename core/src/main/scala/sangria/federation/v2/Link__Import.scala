package sangria.federation.v2

import sangria.schema.ScalarType
import sangria.validation.ValueCoercionViolation

sealed trait Abstract_Link__Import

case class Link__Import(value: String) extends Abstract_Link__Import
case class Link__Import_Object(name: String, as: Option[String] = None)
    extends Abstract_Link__Import

object Link__Import {

  case object Link__Import_Coercion_Violation
      extends ValueCoercionViolation("link_Import scalar expected!!")

  val Type: ScalarType[Abstract_Link__Import] = ScalarType[Abstract_Link__Import](
    name = "link__Import",
    coerceOutput = { (_, _) => "output" },
    coerceUserInput = {
      case obj: Link__Import_Object => Right(obj)
      case str: String => Right(Link__Import(str))
      case _ => Left(Link__Import_Coercion_Violation)
    },
    coerceInput = { _ => Left(Link__Import_Coercion_Violation) }
  )
}
