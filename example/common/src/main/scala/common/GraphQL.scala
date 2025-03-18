package common

import cats.effect._
import cats.implicits._
import io.circe._
import sangria.ast
import sangria.execution._
import sangria.execution.deferred.DeferredResolver
import sangria.marshalling.InputUnmarshaller
import sangria.marshalling.circe.CirceResultMarshaller
import sangria.parser.{QueryParser, SyntaxError}
import sangria.schema.Schema

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

trait GraphQL[F[_], Ctx] {

  def query(request: Json, middleware: List[Middleware[Ctx]]): F[Either[Json, Json]]

  def query(
      query: String,
      operationName: Option[String],
      variables: Json,
      middleware: List[Middleware[Ctx]]
  ): F[Either[Json, Json]]
}

object GraphQL {

  def apply[F[_], Ctx](
      schema: Schema[Ctx, Any],
      deferredResolver: DeferredResolver[Ctx],
      userContext: F[Ctx]
  )(implicit F: Async[F], um: InputUnmarshaller[Json]): GraphQL[F, Ctx] =
    new GraphQL[F, Ctx] {

      private def fail(j: Json): F[Either[Json, Json]] =
        F.pure(j.asLeft)

      def exec(
          schema: Schema[Ctx, Any],
          userContext: F[Ctx],
          query: ast.Document,
          operationName: Option[String],
          variables: Json,
          middleware: List[Middleware[Ctx]]): F[Either[Json, Json]] =
        for {
          ctx <- userContext
          executionContext <- Async[F].executionContext
          execution <- F.attempt(F.fromFuture(F.delay {
            implicit val ec: ExecutionContext = executionContext
            Executor
              .execute(
                schema = schema,
                queryAst = query,
                userContext = ctx,
                variables = variables,
                operationName = operationName,
                middleware = middleware,
                deferredResolver = deferredResolver
              )
          }))
          result <- execution match {
            case Right(json) => F.pure(json.asRight)
            case Left(err: WithViolations) => fail(GraphQLError(err))
            case Left(err) => F.raiseError(err)
          }
        } yield result

      override def query(request: Json, middleware: List[Middleware[Ctx]]): F[Either[Json, Json]] =
        request.hcursor.downField("query").as[String] match {
          case Right(qs) =>
            val operationName =
              request.hcursor.downField("operationName").as[Option[String]].getOrElse(None)
            val variables =
              request.hcursor.downField("variables").as[Json].getOrElse(Json.obj())
            query(qs, operationName, variables, middleware)
          case Left(_) => fail(GraphQLError("No 'query' property was present in the request."))
        }

      override def query(
          query: String,
          operationName: Option[String],
          variables: Json,
          middleware: List[Middleware[Ctx]]): F[Either[Json, Json]] =
        QueryParser.parse(query) match {
          case Success(ast) => exec(schema, userContext, ast, operationName, variables, middleware)
          case Failure(e: SyntaxError) => fail(GraphQLError(e))
          case Failure(e) => F.raiseError(e)
        }
    }
}
