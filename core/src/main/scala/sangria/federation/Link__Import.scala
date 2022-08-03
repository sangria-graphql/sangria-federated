package sangria.federation

import sangria.schema.{ScalarType, StringType}
import sangria.validation.ValueCoercionViolation

sealed trait Abstract_Link__Import

case class Link__Import(value: String) extends Abstract_Link__Import
case class Link__Import_Object(name: String, as: Option[String] = None) extends Abstract_Link__Import

object Link__Import {

  case object Link__ImportCoercionViolation extends ValueCoercionViolation("link_Import scalar expected!!")

  val Type = ScalarType[Abstract_Link__Import](
    name= "link__Import",
    coerceOutput = { case _ =>
      "output"
    },
    coerceUserInput = {
      case obj: Link__Import_Object => Right(obj)
      case str: String => Right(Link__Import(str))
      case _ => Left(Link__ImportCoercionViolation)
    },
    coerceInput = { _ => Left(Link__ImportCoercionViolation) }
  )

    StringType.rename("link_Import").copy(description = None)
}
