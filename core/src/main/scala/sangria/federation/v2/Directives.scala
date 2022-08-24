package sangria.federation.v2

import sangria.ast
import sangria.schema._

object Directives {

  val definitions = List(
    Directive(
      name = "key",
      arguments = List(
        Argument(
          name = "fields",
          argumentType = _FieldSet.Type
        ),
        Argument(
          name = "resolvable",
          argumentType = OptionInputType(BooleanType),
          defaultValue = true
        )),
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
      locations = Set(DirectiveLocation.FieldDefinition)),
    Directive(
      name = "link",
      arguments = List(
        Argument("url", OptionInputType(StringType)),
        Argument("as", OptionInputType(StringType)),
        Argument("for", OptionInputType(Link__Purpose.Type)),
        Argument("import", OptionInputType(ListInputType(OptionInputType(Link__Import.Type))))
      ),
      locations = Set(DirectiveLocation.Schema),
      repeatable = true
    ),
    Directive(
      name = "shareable",
      locations = Set(DirectiveLocation.Object, DirectiveLocation.FieldDefinition)
    )
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
