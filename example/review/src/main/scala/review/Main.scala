package review

import cats.effect._
import cats.implicits._
import common.{GraphQL, Server}
import io.circe.Json
import sangria.federation.v1.Federation
import sangria.schema.Schema

object Main extends IOApp.Simple {

  val env = ReviewService.inMemory

  def graphQL[F[_]: Async]: GraphQL[F] = {
    val (schema, um) = Federation.federate[ReviewService, Any, Json](
      Schema(ReviewAPI.Query),
      sangria.marshalling.circe.CirceInputUnmarshaller)

    GraphQL(schema, env.pure[F])(Async[F], um)
  }

  override def run: IO[Unit] = Server.resource[IO](graphQL, 9082).use(_ => IO.never)
}
