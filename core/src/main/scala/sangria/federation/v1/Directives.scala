package sangria.federation.v1

import sangria.ast
import sangria.schema.{Argument, Directive, DirectiveLocation}

object Directives {

  /** [@key](https://www.apollographql.com/docs/federation/federated-types/federated-directives#key)
    * directive
    */
  object Key {
    val definition: Directive = Directive(
      name = "key",
      arguments = Argument("fields", _FieldSet.Type) :: Nil,
      locations = Set(DirectiveLocation.Object, DirectiveLocation.Interface),
      repeatable = true
    )

    def apply(fields: String): ast.Directive =
      ast.Directive(
        name = "key",
        arguments = Vector(ast.Argument("fields", ast.StringValue(fields))))
  }

  /** [@extends](https://www.apollographql.com/docs/federation/federated-types/federated-directives#extends)
    * directive definition
    */
  val ExtendsDefinition: Directive = Directive(
    name = "extends",
    locations = Set(DirectiveLocation.Object, DirectiveLocation.Interface))

  /** [@extends](https://www.apollographql.com/docs/federation/federated-types/federated-directives#extends)
    * directive
    */
  val Extends: ast.Directive = ast.Directive("extends")

  /** [@requires](https://www.apollographql.com/docs/federation/federated-types/federated-directives#requires)
    * directive
    */
  object Requires {
    val definition: Directive = Directive(
      name = "requires",
      arguments = Argument("fields", _FieldSet.Type) :: Nil,
      locations = Set(DirectiveLocation.FieldDefinition))

    def apply(fields: String): ast.Directive =
      ast.Directive(
        name = "requires",
        arguments = Vector(ast.Argument("fields", ast.StringValue(fields))))
  }

  /** [@external](https://www.apollographql.com/docs/federation/federated-types/federated-directives#external)
    * directive definition
    */
  val ExternalDefinition: Directive =
    Directive(name = "external", locations = Set(DirectiveLocation.FieldDefinition))

  /** [@external](https://www.apollographql.com/docs/federation/federated-types/federated-directives#external)
    * directive
    */
  val External: ast.Directive = ast.Directive("external")

  /** [@provides](https://www.apollographql.com/docs/federation/federated-types/federated-directives#provides)
    * directive
    */
  object Provides {
    val definition: Directive = Directive(
      name = "provides",
      arguments = Argument("fields", _FieldSet.Type) :: Nil,
      locations = Set(DirectiveLocation.FieldDefinition))

    def apply(fields: String): ast.Directive =
      ast.Directive(
        name = "provides",
        arguments = Vector(ast.Argument(name = "fields", value = ast.StringValue(fields))))
  }

  val definitions: List[Directive] = List(
    Key.definition,
    ExternalDefinition,
    ExtendsDefinition,
    Requires.definition,
    Provides.definition
  )
}
