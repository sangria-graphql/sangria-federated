package sangria.federation.v1

import sangria.ast
import sangria.marshalling.{FromInput, InputUnmarshaller}
import sangria.renderer.SchemaFilter
import sangria.schema._
import sangria.util.tag.@@

import scala.collection.mutable

object Federation {
  import Query._

  def federate[Ctx, Val, Node](
      schema: Schema[Ctx, Val],
      um: InputUnmarshaller[Node],
      resolvers: EntityResolver[Ctx, Node]*
  ): (Schema[Ctx, Val], InputUnmarshaller[Node]) = (extend(schema, resolvers), upgrade(um))

  def extend[Ctx, Val, Node](
      schema: Schema[Ctx, Val],
      resolvers: Seq[EntityResolver[Ctx, Node]]): Schema[Ctx, Val] = {
    val resolversMap = resolvers.map(r => r.typename -> r).toMap
    val representationsArg = Argument("representations", ListInputType(_Any.__type[Node]))

    val entities = schema.allTypes.values.collect {
      case obj: ObjectType[Ctx, _] @unchecked if obj.astDirectives.exists(_.name == "key") => obj
    }.toList

    val sdl = Some(schema.renderPretty(SchemaFilter.withoutGraphQLBuiltIn))

    (entities match {
      case Nil =>
        schema.extend(
          ast.Document(definitions = Vector(queryType(_service))),
          AstSchemaBuilder.resolverBased[Ctx](
            FieldResolver.map("Query" -> Map("_service" -> (_ => _Service(sdl)))),
            AdditionalTypes(_Any.__type[Node], _Service.Type, _FieldSet.Type)
          )
        )
      case entities =>
        schema.extend(
          ast.Document(definitions = Vector(queryType(_service, _entities))),
          AstSchemaBuilder.resolverBased[Ctx](
            FieldResolver.map(
              "Query" -> Map(
                "_service" -> (_ => _Service(sdl)),
                "_entities" -> (ctx =>
                  ctx.withArgs(representationsArg) { anys =>
                    val anysWithArgs: Seq[ResolvedAny[Ctx, Node]] =
                      anys.map(any => ResolvedAny(any, resolversMap(any.__typename)))

                    val requestsByTypename = anysWithArgs.groupBy(_.any.__typename)
                    val resolvedBuilder = mutable.Map[ResolvedKey, LeafAction[Ctx, Any]]()
                    requestsByTypename.foreach { case (typename, elements) =>
                      // the resolver is the same for all elements as it's per type
                      val resolver = elements.head.resolver
                      val bulkArgs = elements.map(_.arg)

                      if (bulkArgs.nonEmpty) {
                        resolver.resolve(
                          bulkArgs.asInstanceOf[Seq[resolver.Arg]],
                          ctx.asInstanceOf[Context[Ctx, resolver.Val]]) match {
                          case Value(results) =>
                            results.foreach { result =>
                              val arg = resolver.arg(result)
                              resolvedBuilder(ResolvedKey(typename, arg)) = LeafAction(result)
                            }
                          case e => throw new Exception("expecting Value and got " + e)
                        }
                      }
                    }

                    val resolved = resolvedBuilder.withDefault(_ => LeafAction(None))
                    val results =
                      anysWithArgs.map(a => resolved(ResolvedKey(a.any.__typename, a.arg)))
                    Action.sequence(results)
                  })
              )
            ),
            AdditionalTypes(_Any.__type[Node], _Service.Type, _Entity(entities), _FieldSet.Type)
          )
        )
    }).copy(directives = Directives.definitions ::: schema.directives)
  }

  private case class ResolvedKey(typename: String, arg: Any)
  private case class ResolvedAny[Ctx, Node](
      any: @@[_Any[Node], FromInput.CoercedScalaResult],
      resolver: EntityResolver[Ctx, Node]) {
    val arg: resolver.Arg = any.fields.decode[resolver.Arg](resolver.decoder) match {
      case Right(arg) => arg
      case Left(e) => throw e
    }
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
        if (default.isMapNode(node)) new NodeObject[Node] {

          override def __typename: Option[String] =
            getMapValue(node, "__typename").map(node => getScalarValue(node).asInstanceOf[String])

          override def decode[T](implicit ev: Decoder[Node, T]): Either[Exception, T] =
            ev.decode(node)
        }
        else default.getScalarValue(node)
    }
}
