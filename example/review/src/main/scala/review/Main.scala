package review

import cats.effect._
import cats.implicits._
import common.{GraphQL, Server}
import io.circe.Json
import sangria.federation.v2.Federation
import sangria.renderer.QueryRenderer
import sangria.schema.{BuiltinDirectives, Schema}

object Main extends IOApp.Simple {

  private val env = ReviewService.inMemory

  private def newSchema() =
    Schema(
      query = ReviewAPI.Query,
      directives = common.Directives.Feature.definition :: BuiltinDirectives
    )

  private def graphQL[F[_]: Async]: GraphQL[F, ReviewService] = {
    val (schema, um) = Federation.federate[ReviewService, Any, Json](
      schema = newSchema(),
      composeDirectives = List(common.Directives.Feature.definition.name),
      um = sangria.marshalling.circe.CirceInputUnmarshaller)

    println(QueryRenderer.renderPretty(schema.toAst))
    GraphQL(schema, env.pure[F])(Async[F], um)
  }

  override def run: IO[Unit] = Server.resource[IO, ReviewService](graphQL, 9082).use(_ => IO.never)
}
