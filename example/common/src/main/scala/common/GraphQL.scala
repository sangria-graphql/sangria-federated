package common

import cats.effect._
import cats.implicits._
import io.circe._
import io.circe.optics.JsonPath._
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

  private val queryStringLens = root.query.string
  private val operationNameLens = root.operationName.string
  private val variablesLens = root.variables.obj

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

      def query(request: Json, middleware: List[Middleware[Ctx]]): F[Either[Json, Json]] = {
        val queryString = queryStringLens.getOption(request)
        val operationName = operationNameLens.getOption(request)
        val variables =
          Json.fromJsonObject(variablesLens.getOption(request).getOrElse(JsonObject()))

        queryString match {
          case Some(qs) => query(qs, operationName, variables, middleware)
          case None => fail(GraphQLError("No 'query' property was present in the request."))
        }
      }

      def query(
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
