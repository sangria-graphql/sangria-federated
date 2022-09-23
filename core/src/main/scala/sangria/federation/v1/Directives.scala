package sangria.federation.v1

import sangria.ast
import sangria.schema.{Argument, Directive, DirectiveLocation}

object Directives {

  val definitions: List[Directive] = List(
    Directive(
      name = "key",
      arguments = Argument(
        name = "fields",
        argumentType = _FieldSet.Type
      ) :: Nil,
      locations = Set(DirectiveLocation.Object, DirectiveLocation.Interface),
      repeatable = true
    ),
    Directive(name = "external", locations = Set(DirectiveLocation.FieldDefinition)),
    Directive(
      name = "extends",
      locations = Set(DirectiveLocation.Object, DirectiveLocation.Interface)),
    Directive(
      name = "requires",
      arguments = Argument(
        name = "fields",
        argumentType = _FieldSet.Type
      ) :: Nil,
      locations = Set(DirectiveLocation.FieldDefinition)),
    Directive(
      name = "provides",
      arguments = Argument(
        name = "fields",
        argumentType = _FieldSet.Type
      ) :: Nil,
      locations = Set(DirectiveLocation.FieldDefinition))
  )

  object Key {

    def apply(fields: String): ast.Directive =
      ast.Directive(
        name = "key",
        arguments = Vector(ast.Argument(name = "fields", value = ast.StringValue(fields))))
  }

  val External: ast.Directive = ast.Directive(name = "external", arguments = Vector.empty)

  val Extends: ast.Directive = ast.Directive(name = "extends", arguments = Vector.empty)
}
