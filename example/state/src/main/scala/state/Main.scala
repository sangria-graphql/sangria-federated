package state

import cats.effect._
import cats.implicits._
import com.comcast.ip4s._
import common.{GraphQL, Server}
import io.circe.Json
import org.typelevel.log4cats.{Logger, LoggerFactory}
import org.typelevel.log4cats.slf4j.{Slf4jFactory, Slf4jLogger}
import sangria.federation.v1.Federation
import sangria.schema.Schema

object Main extends IOApp.Simple {

  import StateGraphQLSchema._

  private val env = StateService.inMemory

  private def graphQL[F[_]: Async]: GraphQL[F, StateService] = {
    val (schema, um) = Federation.federate[StateService, Any, Json](
      Schema(StateAPI.Query),
      sangria.marshalling.circe.CirceInputUnmarshaller,
      stateResolver)

    GraphQL(schema, env.pure[F])(Async[F], um)
  }

  override def run: IO[Unit] = run()(Slf4jFactory.create[IO])
  def run()(implicit logger: LoggerFactory[IO]): IO[Unit] =
    Server.resource[IO, StateService](graphQL, port"9081").use(_ => IO.never)
}
