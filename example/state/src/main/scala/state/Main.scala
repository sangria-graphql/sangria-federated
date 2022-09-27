package state

import cats.effect._
import cats.implicits._
import common.{GraphQL, Server}
import io.circe.Json
import sangria.federation.v1.Federation
import sangria.schema.Schema

object Main extends IOApp.Simple {

  import StateGraphQLSchema._

  private val env = StateService.inMemory

  private def graphQL[F[_]: Async]: GraphQL[F, StateService] = {
    val (schema, um) = Federation.federate[StateService, Any, Json](
      Schema(StateAPI.Query),
      sangria.marshalling.circe.CirceInputUnmarshaller,
      stateResolver(env))

    GraphQL(schema, env.pure[F])(Async[F], um)
  }

  override def run: IO[Unit] = Server.resource[IO, StateService](graphQL, 9081).use(_ => IO.never)
}
