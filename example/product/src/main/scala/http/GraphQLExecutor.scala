package http

import io.circe.{Json, JsonObject}
import sangria.ast
import sangria.execution.{Executor, Middleware}
import sangria.marshalling.InputUnmarshaller
import sangria.marshalling.circe.CirceResultMarshaller
import sangria.parser.{QueryParser, SyntaxError}
import sangria.schema.Schema

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait GraphQLExecutor[Ctx] {
  def query(request: Json, middleware: List[Middleware[Ctx]]): Future[Json]
}

object GraphQLExecutor {

  def apply[Ctx](schema: Schema[Ctx, Unit], context: Ctx)(implicit
      um: InputUnmarshaller[Json]): GraphQLExecutor[Ctx] =
    new GraphQLExecutor[Ctx] {
      override def query(request: Json, middleware: List[Middleware[Ctx]]): Future[Json] =
        request.hcursor.downField("query").as[String] match {
          case Right(qs) =>
            val operationName =
              request.hcursor.downField("operationName").as[Option[String]].getOrElse(None)
            val variables =
              request.hcursor.downField("variables").as[Json].getOrElse(Json.obj())
            query(qs, operationName, variables, middleware)
          case Left(_) =>
            Future.successful(GraphQLError("No 'query' property was present in the request."))
        }

      private def query(
          query: String,
          operationName: Option[String],
          variables: Json,
          middleware: List[Middleware[Ctx]]
      ): Future[Json] =
        QueryParser.parse(query) match {
          case Success(ast) => exec(ast, operationName, variables, middleware)
          case Failure(e: SyntaxError) => Future.successful(http.GraphQLError(e))
          case Failure(e) => Future.failed(e)
        }

      private def exec(
          query: ast.Document,
          operationName: Option[String],
          variables: Json,
          middleware: List[Middleware[Ctx]]
      ): Future[Json] =
        Executor.execute(
          schema = schema,
          queryAst = query,
          userContext = context,
          variables = variables,
          operationName = operationName,
          middleware = middleware
        )
    }
}
