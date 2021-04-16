package state

import cats.effect._
import cats.implicits._
import common.{GraphQL, Server}
import io.circe.Json
import sangria.federation.Federation
import sangria.schema.Schema

object Main extends IOApp.Simple {

  import StateGraphQLSchema._

  val env = StateService.inMemory

  def graphQL[F[_]: Async]: GraphQL[F] = {
    val (schema, um) = Federation.federate[StateService, Any, Json](
      Schema(StateAPI.Query),
      sangria.marshalling.circe.CirceInputUnmarshaller,
      stateResolver(env))

    GraphQL(schema, env.pure[F])(Async[F], um)
  }

  override def run: IO[Unit] = Server.resource[IO](graphQL, 9081).use(_ => IO.never)
}
