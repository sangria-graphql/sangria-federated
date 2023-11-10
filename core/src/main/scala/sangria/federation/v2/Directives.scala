package sangria.federation.v2

import sangria.ast
import sangria.schema._
import sangria.util.tag.@@
import sangria.util.tag

object Directives {

  trait Link

  object Link {
    val definition: Directive @@ Link = tag[Link](
      Directive(
        name = "link",
        arguments = List(
          Argument("url", StringType),
          Argument("as", OptionInputType(StringType)),
          Argument("for", OptionInputType(Link__Purpose.Type)),
          Argument("import", OptionInputType(ListInputType(OptionInputType(Link__Import.Type))))
        ),
        locations = Set(DirectiveLocation.Schema),
        repeatable = true
      ))

    def apply(url: String, as: String): ast.Directive @@ Link =
      apply(url = url, as = Some(as))

    def apply(url: String, `for`: Link__Purpose.Value): ast.Directive @@ Link =
      apply(url = url, `for` = Some(`for`))

    def apply(
        url: String,
        as: Option[String] = None,
        `for`: Option[Link__Purpose.Value] = None,
        `import`: Option[Vector[Abstract_Link__Import]] = None): ast.Directive @@ Link =
      tag[Link](
        ast.Directive(
          name = "link",
          arguments = Vector(
            Some(ast.Argument("url", ast.StringValue(url))),
            as.map(v => ast.Argument("as", ast.StringValue(v))),
            `for`.map(v => ast.Argument("for", ast.EnumValue(Link__Purpose.Type.coerceOutput(v)))),
            `import`.map(v =>
              ast.Argument("import", ast.ListValue(v.map(v => Abstract_Link__Import.toAst(v)))))
          ).flatten
        ))
  }

  /** [@key](https://www.apollographql.com/docs/federation/federated-types/federated-directives#key)
    * directive
    */
  object Key {
    val definition: Directive = Directive(
      name = "key",
      arguments = List(
        Argument("fields", _FieldSet.Type),
        Argument("resolvable", OptionInputType(BooleanType), defaultValue = true)),
      locations = Set(DirectiveLocation.Object, DirectiveLocation.Interface),
      repeatable = true
    )

    def apply(fields: String, resolvable: Boolean): ast.Directive =
      apply(fields, resolvable = Some(resolvable))

    def apply(fields: String, resolvable: Option[Boolean] = None): ast.Directive =
      ast.Directive(
        name = "key",
        arguments = Vector(
          Some(ast.Argument("fields", ast.StringValue(fields))),
          resolvable.map(r => ast.Argument("resolvable", ast.BooleanValue(r)))).flatten
      )
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
  val Extends: ast.Directive = ast.Directive(name = "extends")

  /** [@shareable](https://www.apollographql.com/docs/federation/federated-types/federated-directives#shareable)
    * directive definition
    */
  val ShareableDefinition: Directive = Directive(
    name = "shareable",
    locations = Set(DirectiveLocation.Object, DirectiveLocation.FieldDefinition),
    repeatable = true
  )

  /** [@shareable](https://www.apollographql.com/docs/federation/federated-types/federated-directives#shareable)
    * directive
    */
  val Shareable: ast.Directive = ast.Directive(name = "shareable")

  /** [@inaccessible](https://www.apollographql.com/docs/federation/federated-types/federated-directives#inaccessible)
    * directive definition
    */
  val InaccessibleDefinition: Directive = Directive(
    name = "inaccessible",
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
    )
  )

  /** [@inaccessible](https://www.apollographql.com/docs/federation/federated-types/federated-directives#inaccessible)
    * directive
    */
  val Inaccessible: ast.Directive = ast.Directive(name = "inaccessible")

  /** [@override](https://www.apollographql.com/docs/federation/federated-types/federated-directives#override)
    * directive
    */
  object Override {
    val Definition: Directive = Directive(
      name = "override",
      arguments = List(Argument("from", StringType)),
      locations = Set(DirectiveLocation.FieldDefinition)
    )

    def apply(from: String): ast.Directive =
      ast.Directive(
        name = "override",
        arguments = Vector(ast.Argument("from", ast.StringValue(from))))
  }

  /** [@external](https://www.apollographql.com/docs/federation/federated-types/federated-directives#external)
    * directive definition
    */
  val ExternalDefinition: Directive =
    Directive(name = "external", locations = Set(DirectiveLocation.FieldDefinition))

  /** [@external](https://www.apollographql.com/docs/federation/federated-types/federated-directives#external)
    * directive
    */
  val External: ast.Directive = ast.Directive(name = "external")

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
        arguments = Vector(ast.Argument("fields", ast.StringValue(fields))))
  }

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

  /** [@tag](https://www.apollographql.com/docs/federation/federated-types/federated-directives#tag)
    * directive
    */
  object Tag {
    val definition: Directive = Directive(
      name = "tag",
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
      ast.Directive(name = "tag", arguments = Vector(ast.Argument("name", ast.StringValue(name))))
  }

  trait ComposeDirective

  /** [@composeDirective](https://www.apollographql.com/docs/federation/federated-types/federated-directives#composedirective)
    */
  object ComposeDirective {
    val definition: Directive @@ ComposeDirective = tag[ComposeDirective](
      Directive(
        name = "composeDirective",
        arguments = List(Argument("name", StringType)),
        locations = Set(DirectiveLocation.Schema),
        repeatable = true
      ))

    def apply(name: String): ast.Directive @@ ComposeDirective =
      tag[ComposeDirective](
        ast.Directive(
          name = "composeDirective",
          arguments = Vector(ast.Argument("name", ast.StringValue(name)))))

    def apply(directive: Directive): ast.Directive @@ ComposeDirective =
      apply("@" + directive.name)
  }
}
