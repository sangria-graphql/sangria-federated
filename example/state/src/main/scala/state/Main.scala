package state

import cats.effect._
import cats.implicits._
import com.comcast.ip4s._
import common.{GraphQL, Server}
import io.circe.Json
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import sangria.execution.deferred.DeferredResolver
import sangria.federation.v2.Federation
import sangria.schema.Schema

import scala.concurrent.ExecutionContext

object Main extends IOApp.Simple {

  import StateGraphQLSchema._

  private val ctx = StateContext(StateService.inMemory, ExecutionContext.global)

  private def graphQL[F[_]: Async]: GraphQL[F, StateContext] = {
    val (schema, um) = Federation.federate[StateContext, Any, Json](
      Schema(StateGraphQLSchema.Query),
      sangria.marshalling.circe.CirceInputUnmarshaller,
      stateResolver)

    GraphQL(schema, DeferredResolver.fetchers(StateGraphQLSchema.states), ctx.pure[F])(Async[F], um)
  }

  override def run: IO[Unit] = run(Slf4jLogger.getLogger[IO])
  def run(logger: Logger[IO]): IO[Unit] =
    Server.resource[IO, StateContext](logger, graphQL, port"9081").use(_ => IO.never)
}
