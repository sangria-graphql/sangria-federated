package sangria.federation.v2

import sangria.ast
import sangria.schema.ScalarType
import sangria.validation.ValueCoercionViolation

sealed trait Abstract_Link__Import
object Abstract_Link__Import {
  def toAst(linkImport: Abstract_Link__Import): ast.Value =
    linkImport match {
      case Link__Import(value) => ast.StringValue(value)
      case Link__Import_Object(name, as) =>
        new ast.ObjectValue(
          Vector[Option[ast.ObjectField]](
            Some(ast.ObjectField("name", ast.StringValue(name))),
            as.map(a => ast.ObjectField("as", ast.StringValue(a)))
          ).flatten)
    }
}

case class Link__Import(value: String) extends Abstract_Link__Import {
  def as(as: String): Link__Import_Object = new Link__Import_Object(name = value, as = Some(as))
}
case class Link__Import_Object(name: String, as: Option[String]) extends Abstract_Link__Import
object Link__Import_Object {
  def apply(name: String): Link__Import_Object = new Link__Import_Object(name = name, as = None)
  def apply(name: String, as: String): Link__Import_Object =
    new Link__Import_Object(name = name, as = Some(as))
}

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
