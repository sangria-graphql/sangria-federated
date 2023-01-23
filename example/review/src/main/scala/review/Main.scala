package review

import cats.effect._
import cats.implicits._
import com.comcast.ip4s._
import common.{CustomDirectives, GraphQL, Server}
import io.circe.Json
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import sangria.federation.v2.{CustomDirectivesDefinition, Federation, Spec}
import sangria.schema.Schema

object Main extends IOApp.Simple {

  private val env = ReviewService.inMemory

  private def graphQL[F[_]: Async]: GraphQL[F, ReviewService] = {
    val (schema, um) = Federation.federate[ReviewService, Any, Json](
      Schema(ReviewAPI.Query),
      customDirectives = CustomDirectivesDefinition(
        Spec("https://myspecs.dev/myDirective/v1.0") -> List(CustomDirectives.Feature.definition)),
      sangria.marshalling.circe.CirceInputUnmarshaller
    )

    GraphQL(schema, env.pure[F])(Async[F], um)
  }

  override def run: IO[Unit] = run(Slf4jLogger.getLogger[IO])
  def run(logger: Logger[IO]): IO[Unit] =
    Server.resource[IO, ReviewService](logger, graphQL, port"9082").use(_ => IO.never)
}
