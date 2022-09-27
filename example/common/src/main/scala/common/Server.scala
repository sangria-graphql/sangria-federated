package common

import cats.data.NonEmptyList
import cats.effect._
import cats.implicits._
import io.circe.Json
import org.http4s._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.circe._
import org.http4s.dsl._
import org.http4s.headers.Location
import org.http4s.implicits._
import org.http4s.server.Server
import org.typelevel.ci.CIString
import sangria.execution.Middleware
import sangria.federation.tracing.ApolloFederationTracing

object Server {

  /** https://www.apollographql.com/docs/federation/metrics/#how-tracing-data-is-exposed-from-a-subgraph
    */
  private val `apollo-federation-include-trace`: Header.Raw = {
    val name: CIString = CIString("apollo-federation-include-trace")
    Header.Raw(name, "ftv1")
  }

  def resource[F[_]: Async, Ctx](
      graphQL: GraphQL[F, Ctx],
      port: Int
  ): Resource[F, Server] = {

    object dsl extends Http4sDsl[F]
    import dsl._

    val routes: HttpRoutes[F] = HttpRoutes.of[F] {
      case req @ POST -> Root / "api" / "graphql" =>
        val tracing = req.headers
          .get(`apollo-federation-include-trace`.name)
          .contains(NonEmptyList.one(`apollo-federation-include-trace`))
        val middleware: List[Middleware[Ctx]] = if (tracing) ApolloFederationTracing :: Nil else Nil
        req.as[Json].flatMap(json => graphQL.query(json, middleware)).flatMap {
          case Right(json) => Ok(json)
          case Left(json) => BadRequest(json)
        }
      case GET -> Root / "playground" =>
        StaticFile
          .fromResource[F]("/playground.html")
          .getOrElseF(NotFound())

      case _ =>
        PermanentRedirect(Location(uri"/playground"))
    }

    BlazeServerBuilder[F]
      .bindHttp(port, "localhost")
      .withHttpApp(routes.orNotFound)
      .resource
  }
}
