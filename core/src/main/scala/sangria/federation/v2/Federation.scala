package sangria.federation.v2

import sangria.ast
import sangria.federation.v2.Directives.{ComposeDirective, Link}
import sangria.marshalling.InputUnmarshaller
import sangria.renderer.SchemaFilter
import sangria.schema._
import sangria.util.tag.@@

case class Spec(url: String) extends AnyVal

/** Those custom directives will be exposed as
  * [[https://www.apollographql.com/docs/federation/federated-types/federated-directives#composedirective `@composeDirectives`]]
  */
case class CustomDirectivesDefinition(included: Map[Spec, List[Directive]])
object CustomDirectivesDefinition {
  def apply(item: (Spec, List[Directive])*): CustomDirectivesDefinition =
    new CustomDirectivesDefinition(item.toMap)
}

object Federation {
  import Query._

  def federate[Ctx, Val, Node](
      schema: Schema[Ctx, Val],
      um: InputUnmarshaller[Node],
      resolvers: EntityResolver[Ctx, Node]*
  ): (Schema[Ctx, Val], InputUnmarshaller[Node]) =
    (extend(schema, Nil, Nil, resolvers), upgrade(um))

  def federate[Ctx, Val, Node](
      schema: Schema[Ctx, Val],
      customDirectives: CustomDirectivesDefinition,
      um: InputUnmarshaller[Node],
      resolvers: EntityResolver[Ctx, Node]*
  ): (Schema[Ctx, Val], InputUnmarshaller[Node]) =
    (extend(schema, customDirectives, resolvers), upgrade(um))

  def extend[Ctx, Val, Node](
      schema: Schema[Ctx, Val],
      resolvers: Seq[EntityResolver[Ctx, Node]]): Schema[Ctx, Val] =
    extend(schema, Nil, Nil, resolvers)

  /** High level API allowing to expose custom directives
    */
  def extend[Ctx, Val, Node](
      schema: Schema[Ctx, Val],
      customDirectives: CustomDirectivesDefinition,
      resolvers: Seq[EntityResolver[Ctx, Node]]): Schema[Ctx, Val] = {

    val additionalLinkImports: List[ast.Directive @@ Link] =
      customDirectives.included.iterator.map { case (spec, directives) =>
        Directives.Link(
          url = spec.url,
          `import` = Some(directives.iterator.map(d => Link__Import("@" + d.name)).toVector)
        )
      }.toList
    val composeDirectives: List[ast.Directive @@ ComposeDirective] =
      customDirectives.included.values.flatMap { directives =>
        directives.map(d => Directives.ComposeDirective(d))
      }.toList
    val schemaWithCustomDirectives =
      schema.copy(directives = schema.directives ++ customDirectives.included.values.flatten)
    extend(schemaWithCustomDirectives, additionalLinkImports, composeDirectives, resolvers)
  }

  def extend[Ctx, Val, Node](
      schema: Schema[Ctx, Val],
      additionalLinkImports: List[ast.Directive @@ Link],
      composeDirectives: List[ast.Directive @@ ComposeDirective],
      resolvers: Seq[EntityResolver[Ctx, Node]]): Schema[Ctx, Val] = {
    val resolversMap = resolvers.map(r => r.typename -> r).toMap
    val representationsArg = Argument("representations", ListInputType(_Any.__type[Node]))

    val entities = schema.allTypes.values.collect {
      case obj: ObjectType[Ctx, _] @unchecked if obj.astDirectives.exists(_.name == "key") => obj
    }.toList

    val federationDirectives: List[Directive] = List(
      Directives.Key.definition,
      Directives.InterfaceObjectDefinition,
      Directives.ExtendsDefinition,
      Directives.ShareableDefinition,
      Directives.InaccessibleDefinition,
      Directives.Override.Definition,
      Directives.ExternalDefinition,
      Directives.Provides.definition,
      Directives.Requires.definition,
      Directives.Tag.definition
    )

    val importedDirectives: List[Directive] =
      if (composeDirectives.nonEmpty)
        Directives.ComposeDirective.definition :: federationDirectives
      else
        federationDirectives

    val federationV2Link = Directives.Link(
      url = "https://specs.apollo.dev/federation/v2.3",
      `import` = Some(importedDirectives.map(d => Link__Import("@" + d.name)).toVector)
    )

    val addedDirectives: Vector[ast.Directive] =
      (federationV2Link :: additionalLinkImports ::: composeDirectives).toVector

    val extendedSchema = schema.copy(astDirectives = addedDirectives)
    val sdl = Some(extendedSchema.renderPretty(SchemaFilter.withoutGraphQLBuiltIn))

    (entities match {
      case Nil =>
        extendedSchema.extend(
          ast.Document(Vector(queryType(_service))),
          AstSchemaBuilder.resolverBased[Ctx](
            FieldResolver.map("Query" -> Map("_service" -> (_ => _Service(sdl)))),
            AdditionalTypes(
              _Any.__type[Node],
              Link__Import.Type,
              _Service.Type,
              _FieldSet.Type,
              Link__Purpose.Type)
          )
        )
      case entities =>
        extendedSchema.extend(
          ast.Document(Vector(queryType(_service, _entities))),
          AstSchemaBuilder.resolverBased[Ctx](
            FieldResolver.map(
              "Query" -> Map(
                "_service" -> (_ => _Service(sdl)),
                "_entities" -> (ctx =>
                  ctx.withArgs(representationsArg) { anys =>
                    Action.sequence(anys.map { any =>
                      val typeName = any.__typename
                      val resolver = resolversMap.getOrElse(
                        typeName,
                        throw new Exception(s"no resolver found for type '$typeName'"))

                      any.fields.decode[resolver.Arg](resolver.decoder) match {
                        case Right(value) => resolver.resolve(value, ctx)
                        case Left(_) => LeafAction(None)
                      }
                    })
                  })
              )
            ),
            AdditionalTypes(
              _Any.__type[Node],
              Link__Import.Type,
              _Service.Type,
              _Entity(entities),
              _FieldSet.Type,
              Link__Purpose.Type)
          )
        )
    }).copy(directives =
      Directives.Link.definition :: federationDirectives ::: extendedSchema.directives)
  }

  def upgrade[Node](default: InputUnmarshaller[Node]): InputUnmarshaller[Node] =
    new InputUnmarshaller[Node] {

      override def getRootMapValue(node: Node, key: String): Option[Node] =
        default.getRootMapValue(node, key)
      override def isMapNode(node: Node): Boolean = default.isMapNode(node)
      override def getMapValue(node: Node, key: String): Option[Node] =
        default.getMapValue(node, key)
      override def getMapKeys(node: Node): Traversable[String] = default.getMapKeys(node)

      override def isListNode(node: Node): Boolean = default.isListNode(node)
      override def getListValue(node: Node): Seq[Node] = default.getListValue(node)

      override def isDefined(node: Node): Boolean = default.isDefined(node)
      override def isEnumNode(node: Node): Boolean = default.isEnumNode(node)
      override def isVariableNode(node: Node): Boolean = default.isVariableNode(node)

      override def getScalaScalarValue(node: Node): Any =
        default.getScalaScalarValue(node)

      override def getVariableName(node: Node): String = default.getVariableName(node)

      override def render(node: Node): String = default.render(node)

      override def isScalarNode(node: Node): Boolean =
        default.isMapNode(node) || default.isScalarNode(node)
      override def getScalarValue(node: Node): Any =
        if (default.isMapNode(node)) {
          if (getMapValue(node, "__typename").isDefined) {
            new NodeObject[Node] {

              override def __typename: Option[String] =
                getMapValue(node, "__typename").map(node =>
                  getScalarValue(node).asInstanceOf[String])

              override def decode[T](implicit ev: Decoder[Node, T]): Either[Exception, T] =
                ev.decode(node)
            }
          } else {
            getMapValue(node, "name") match {
              case Some(name) =>
                getScalaScalarValue(name) match {
                  case name: String =>
                    getMapValue(node, "as") match {
                      case Some(as) =>
                        getScalaScalarValue(as) match {
                          case as: String =>
                            Link__Import_Object(name, Some(as))
                          case _ => Link__Import_Object(name)
                        }
                      case None =>
                        Link__Import_Object(name)
                    }
                  case _ =>
                    default.getScalarValue(node)
                }
              case None =>
                default.getScalarValue(node)
            }
          }
        } else default.getScalarValue(node)
    }
}
