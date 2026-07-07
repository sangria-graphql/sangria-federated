package sangria.federation.v2

import sangria.ast
import sangria.federation.v2._Any
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

    val usedDirectiveNames: Set[String] = usedDirectives(schema)

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
      Directives.Tag.definition,
      Directives.AuthenticatedDefinition,
      Directives.RequiresScopes.definition,
      Directives.Policy.definition,
      Directives.Context.definition,
      Directives.FromContext.definition,
      Directives.Cost.definition,
      Directives.ListSize.definition
    ).filter(d => usedDirectiveNames.contains(d.name))

    val conditionalScalarTypes: List[Type with Named] = List(
      usedDirectiveNames.contains("requiresScopes") -> Federation__Scope.Type,
      usedDirectiveNames.contains("policy") -> Federation__Policy.Type,
      usedDirectiveNames.contains("fromContext") -> Federation__ContextFieldValue.Type
    ).collect { case (true, tpe) => tpe }

    val importedDirectives: List[Directive] =
      if (composeDirectives.nonEmpty)
        Directives.ComposeDirective.definition :: federationDirectives
      else
        federationDirectives

    val minorVersion: Int =
      (0 :: importedDirectives.map(d => minorVersionOf.getOrElse(d.name, 0))).max

    val federationV2Link = Directives.Link(
      url = s"https://specs.apollo.dev/federation/v2.$minorVersion",
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
            AdditionalTypes[Ctx](
              (List[Type with Named](
                _Any.__type[Node],
                Link__Import.Type,
                _Service.Type,
                _FieldSet.Type,
                Link__Purpose.Type
              ) ++ conditionalScalarTypes): _*
            )
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
                  ctx.withArgs(representationsArg) { (anys: Seq[_Any[Node]]) =>
                    Action.sequence(anys.map { (any: _Any[Node]) =>
                      val typeName = any.__typename
                      val resolver = resolversMap.getOrElse(
                        typeName,
                        throw new Exception(s"no resolver found for type '$typeName'"))

                      any.fields.decode[resolver.Arg](resolver.decoder) match {
                        case Right(value) => resolver.resolve(value, ctx)
                        case Left(e) => throw e
                      }
                    })
                  })
              )
            ),
            AdditionalTypes[Ctx](
              (List[Type with Named](
                _Any.__type[Node],
                Link__Import.Type,
                _Service.Type,
                _Entity(entities),
                _FieldSet.Type,
                Link__Purpose.Type
              ) ++ conditionalScalarTypes): _*
            )
          )
        )
    }).copy(directives = Directives.Link.definition :: extendedSchema.directives)
  }

  /** Minor version of the [[https://specs.apollo.dev/federation/ federation spec]] that first
    * introduced each directive. Directives not listed here are available since `v2.0`.
    */
  private val minorVersionOf: Map[String, Int] = Map(
    (Directives.ComposeDirective.definition: Directive).name -> 1,
    Directives.InterfaceObjectDefinition.name -> 3,
    Directives.AuthenticatedDefinition.name -> 5,
    Directives.RequiresScopes.definition.name -> 5,
    Directives.Policy.definition.name -> 6,
    Directives.Context.definition.name -> 8,
    Directives.FromContext.definition.name -> 8,
    Directives.Cost.definition.name -> 9,
    Directives.ListSize.definition.name -> 9
  )

  /** Collects the names of all directives applied anywhere in the given schema (on types, fields,
    * arguments, enum values and input fields), so that only the federation directives that are
    * actually used need to be imported via `@link`.
    */
  private def usedDirectives[Ctx, Val](schema: Schema[Ctx, Val]): Set[String] = {
    val names = Set.newBuilder[String]

    def addAll(directives: Iterable[ast.Directive]): Unit = names ++= directives.map(_.name)

    def addField(field: Field[Ctx, _]): Unit = {
      addAll(field.astDirectives)
      field.arguments.foreach(a => addAll(a.astDirectives))
    }

    schema.allTypes.values.foreach {
      case t: ObjectType[Ctx, _] @unchecked =>
        addAll(t.astDirectives)
        t.ownFields.foreach(addField)
      case t: InterfaceType[Ctx, _] @unchecked =>
        addAll(t.astDirectives)
        t.ownFields.foreach(addField)
      case t: InputObjectType[_] =>
        addAll(t.astDirectives)
        t.fields.foreach(f => addAll(f.astDirectives))
      case t: EnumType[_] =>
        addAll(t.astDirectives)
        t.values.foreach(v => addAll(v.astDirectives))
      case t: HasAstInfo => addAll(t.astDirectives)
      case _ => ()
    }

    names.result()
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
