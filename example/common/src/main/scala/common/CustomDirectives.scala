package common

import sangria.schema._
import sangria.ast

object CustomDirectives {
  object Feature {
    val definition: Directive = Directive(
      name = "feature",
      arguments = List(
        Argument("name", StringType)
      ),
      locations = Set(
        DirectiveLocation.FieldDefinition,
        DirectiveLocation.Object,
        DirectiveLocation.Interface,
        DirectiveLocation.Union,
        DirectiveLocation.ArgumentDefinition,
        DirectiveLocation.Scalar,
        DirectiveLocation.Enum,
        DirectiveLocation.EnumValue,
        DirectiveLocation.InputObject,
        DirectiveLocation.InputFieldDefinition
      ),
      repeatable = true
    )

    def apply(name: String): ast.Directive =
      ast.Directive(
        name = "feature",
        arguments = Vector(ast.Argument("name", ast.StringValue(name))))
  }
}
