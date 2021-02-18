package core

import cats.effect._
import cats.implicits._
import io.circe.Json
import sangria.federation.Federation
import sangria.schema.Schema

object Main extends IOApp {

  val env = ReviewService.inMemory

  def graphQL[F[_]: Effect]: GraphQL[F] = {
    val (schema, um) = Federation.federate[ReviewService.ReviewService, Json](
      Schema(ReviewAPI.Query),
      sangria.marshalling.circe.CirceInputUnmarshaller)

    GraphQL(schema, env.pure[F])(implicitly[Effect[F]], um)
  }

  def run(args: List[String]): IO[ExitCode] =
    (for {
      blocker <- Blocker[IO]
      server <- Server.resource[IO](graphQL, blocker, 9082)
    } yield server).use(_ => IO.never.as(ExitCode.Success))
}
