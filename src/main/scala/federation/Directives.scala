package federation

import sangria.ast
import sangria.schema._

object Directives {

  val definitions = List(
    Directive(
      name = "key",
      arguments = Argument(
        name = "fields",
        argumentType = _FieldSet.Type
      ) :: Nil,
      locations = Set(DirectiveLocation.Object, DirectiveLocation.Interface)),
    Directive(name = "external", locations = Set(DirectiveLocation.FieldDefinition)),
    Directive(
      name = "extends",
      locations = Set(DirectiveLocation.Object, DirectiveLocation.Interface)),
    Directive(name = "requires", locations = Set(DirectiveLocation.FieldDefinition)),
    Directive(name = "provides", locations = Set(DirectiveLocation.FieldDefinition))
  )

  object Key {

    def apply(fields: String) =
      ast.Directive(
        name = "key",
        arguments = Vector(ast.Argument(name = "fields", value = ast.StringValue(fields))))
  }

  val External = ast.Directive(name = "external", arguments = Vector.empty)

  val Extends = ast.Directive(name = "extends", arguments = Vector.empty)
}
