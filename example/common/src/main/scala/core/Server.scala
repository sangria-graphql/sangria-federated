package core

import cats.effect._
import cats.implicits._
import io.circe.Json
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl._
import org.http4s.headers.Location
import org.http4s.implicits._
import org.http4s.server.blaze._
import org.http4s.server.Server

object Server {

  def resource[F[_]: ConcurrentEffect: ContextShift: Timer](
      graphQL: GraphQL[F],
      blocker: Blocker,
      port: Int
  ): Resource[F, Server[F]] = {

    object dsl extends Http4sDsl[F]
    import dsl._
    import scala.concurrent.ExecutionContext.global

    val routes: HttpRoutes[F] = HttpRoutes.of[F] {
      case req @ POST -> Root / "api" / "graphql" =>
        req.as[Json].flatMap(graphQL.query).flatMap {
          case Right(json) => Ok(json)
          case Left(json) => BadRequest(json)
        }
      case GET -> Root / "playground" =>
        StaticFile
          .fromResource[F]("/playground.html", blocker)
          .getOrElseF(NotFound())

      case _ =>
        PermanentRedirect(Location(uri"/playground"))
    }

    BlazeServerBuilder[F](global)
      .bindHttp(port, "localhost")
      .withHttpApp(routes.orNotFound)
      .resource
  }
}
