package state

import cats.effect._
import cats.implicits._
import common.{GraphQL, Server}
import io.circe.Json
import sangria.federation.Federation
import sangria.schema.Schema

object Main extends IOApp {

  import StateGraphQLSchema._

  val env = StateService.inMemory

  def graphQL[F[_]: Effect]: GraphQL[F] = {
    val (schema, um) = Federation.federate[StateService, Json](
      Schema(StateAPI.Query),
      sangria.marshalling.circe.CirceInputUnmarshaller,
      stateResolver(env))

    GraphQL(schema, env.pure[F])(implicitly[Effect[F]], um)
  }

  def run(args: List[String]): IO[ExitCode] =
    (for {
      blocker <- Blocker[IO]
      server <- Server.resource[IO](graphQL, blocker, 9081)
    } yield server).use(_ => IO.never.as(ExitCode.Success))
}
