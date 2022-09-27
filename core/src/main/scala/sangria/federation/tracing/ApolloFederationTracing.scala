package sangria.federation.tracing

import com.google.protobuf.timestamp.Timestamp
import reports.Trace
import reports.Trace.{Location, Node}
import sangria.ast._
import sangria.execution._
import sangria.marshalling.queryAst._
import sangria.renderer.SchemaRenderer
import sangria.schema.Context
import sangria.validation.AstNodeLocation

import java.time.Instant
import java.util.Base64
import java.util.concurrent.ConcurrentLinkedQueue

/** Implements https://www.apollographql.com/docs/federation/metrics/
  */
object ApolloFederationTracing
    extends Middleware[Any]
    with MiddlewareAfterField[Any]
    with MiddlewareErrorField[Any]
    with MiddlewareExtension[Any] {
  type QueryVal = QueryTrace
  type FieldVal = Long

  def beforeQuery(context: MiddlewareQueryContext[Any, _, _]): QueryTrace =
    QueryTrace(Instant.now(), System.nanoTime(), new ConcurrentLinkedQueue)

  def afterQuery(queryVal: QueryVal, context: MiddlewareQueryContext[Any, _, _]): Unit = ()

  def beforeField(
      queryVal: QueryVal,
      mctx: MiddlewareQueryContext[Any, _, _],
      ctx: Context[Any, _]): BeforeFieldResult[Any, FieldVal] =
    continue(System.nanoTime())

  def afterField(
      queryVal: QueryVal,
      fieldVal: FieldVal,
      value: Any,
      mctx: MiddlewareQueryContext[Any, _, _],
      ctx: Context[Any, _]): None.type = {
    queryVal.fieldData.add(metricNode(queryVal, fieldVal, ctx))
    None
  }

  def fieldError(
      queryVal: QueryVal,
      fieldVal: FieldVal,
      error: Throwable,
      mctx: MiddlewareQueryContext[Any, _, _],
      ctx: Context[Any, _]): Unit = {
    var node = metricNode(queryVal, fieldVal, ctx)
    error match {
      case e: AstNodeLocation =>
        node = node.copy(error = Trace.Error(
          message = e.simpleErrorMessage,
          location = e.locations.map(l => Location(l.line, l.column))) :: Nil)
      case e: UserFacingError =>
        node = node.copy(error = Trace.Error(message = e.getMessage) :: Nil)
      case _ => ()
    }
    queryVal.fieldData.add(node)
  }

  private def metricNode(queryVal: QueryVal, fieldVal: FieldVal, ctx: Context[Any, _]): Node =
    Node(
      id = Node.Id.ResponseName(ctx.field.name),
      startTime = fieldVal - queryVal.startNanos,
      endTime = System.nanoTime() - queryVal.startNanos,
      `type` = SchemaRenderer.renderTypeName(ctx.field.fieldType),
      parentType = ctx.parentType.name,
      originalFieldName = ctx.field.name
    )

  def afterQueryExtensions(
      queryVal: QueryVal,
      context: MiddlewareQueryContext[Any, _, _]): Vector[Extension[_]] = {
    val startNanos = queryVal.startNanos
    val endNanos = System.nanoTime()
    val root = Trace(
      startTime = Some(toTimestamp(startNanos)),
      endTime = Some(toTimestamp(endNanos)),
      durationNs = endNanos - startNanos,
      root = None
    )
    val rootSerialized = StringValue(new String(Base64.getEncoder.encode(root.toByteArray)))
    Vector(Extension(ObjectValue("ftv1" -> rootSerialized): Value))
  }

  private def toTimestamp(epochMilli: Long): Timestamp =
    Timestamp.of(
      epochMilli / 1000,
      (epochMilli % 1000).toInt * 1000000
    )

  case class QueryTrace(
      startTime: Instant,
      startNanos: Long,
      fieldData: ConcurrentLinkedQueue[Node])
}
