package core

import scala.util.{Failure, Success}

import cats.effect._
import cats.implicits._
import io.circe._
import io.circe.optics.JsonPath._
import sangria.ast.Document
import sangria.execution._
import sangria.marshalling.InputUnmarshaller
import sangria.marshalling.circe.CirceResultMarshaller
import sangria.parser.{QueryParser, SyntaxError}
import sangria.schema.Schema
import sangria.validation.AstNodeViolation

trait GraphQL[F[_]] {

  def query(request: Json): F[Either[Json, Json]]

  def query(
      query: String,
      operationName: Option[String],
      variables: Json
  ): F[Either[Json, Json]]
}

object GraphQL {

  private val queryStringLens = root.query.string
  private val operationNameLens = root.operationName.string
  private val variablesLens = root.variables.obj

  private def formatString(s: String): Json = Json.obj(
    "errors" -> Json.arr(
      Json.obj("message" -> Json.fromString(s))
    ))

  private def formatSyntaxError(e: SyntaxError): Json = Json.obj(
    "errors" -> Json.arr(Json.obj(
      "message" -> Json.fromString(e.getMessage),
      "locations" -> Json.arr(Json.obj(
        "line" -> Json.fromInt(e.originalError.position.line),
        "column" -> Json.fromInt(e.originalError.position.column)))
    )))

  private def formatThrowable(e: Throwable): Json = Json.obj(
    "errors" -> Json.arr(
      Json.obj(
        "class" -> Json.fromString(e.getClass.getName),
        "message" -> Json.fromString(e.getMessage))))

  private def formatWithViolations(e: WithViolations): Json =
    Json.obj("errors" -> Json.fromValues(e.violations.map {
      case v: AstNodeViolation =>
        Json.obj(
          "message" -> Json.fromString(v.errorMessage),
          "locations" -> Json.fromValues(v.locations.map(loc =>
            Json.obj("line" -> Json.fromInt(loc.line), "column" -> Json.fromInt(loc.column))))
        )
      case v => Json.obj("message" -> Json.fromString(v.errorMessage))
    }))

  def apply[F[_], A](
      schema: Schema[A, Any],
      userContext: F[A]
  )(implicit F: Async[F], um: InputUnmarshaller[Json]): GraphQL[F] =
    new GraphQL[F] {

      import scala.concurrent.ExecutionContext.Implicits.global

      def fail(j: Json): F[Either[Json, Json]] =
        F.pure(j.asLeft)

      def exec(
          schema: Schema[A, Any],
          userContext: F[A],
          query: Document,
          operationName: Option[String],
          variables: Json): F[Either[Json, Json]] =
        userContext
          .flatMap { ctx =>
            F.async { (cb: Either[Throwable, Json] => Unit) =>
              Executor
                .execute(
                  schema = schema,
                  queryAst = query,
                  userContext = ctx,
                  variables = variables,
                  operationName = operationName,
                  exceptionHandler = ExceptionHandler { case (_, e) =>
                    HandledException(e.getMessage)
                  }
                )
                .onComplete {
                  case Success(value) => cb(Right(value))
                  case Failure(error) => cb(Left(error))
                }
            }
          }
          .attempt
          .flatMap {
            case Right(json) => F.pure(json.asRight)
            case Left(err: WithViolations) => fail(formatWithViolations(err))
            case Left(err) => fail(formatThrowable(err))
          }

      def query(request: Json): F[Either[Json, Json]] = {
        val queryString = queryStringLens.getOption(request)
        val operationName = operationNameLens.getOption(request)
        val variables =
          Json.fromJsonObject(variablesLens.getOption(request).getOrElse(JsonObject()))

        queryString match {
          case Some(qs) => query(qs, operationName, variables)
          case None => fail(formatString("No 'query' property was present in the request."))
        }
      }

      def query(
          query: String,
          operationName: Option[String],
          variables: Json): F[Either[Json, Json]] =
        QueryParser.parse(query) match {
          case Success(ast) => exec(schema, userContext, ast, operationName, variables)
          case Failure(e @ SyntaxError(_, _, pe)) => fail(formatSyntaxError(e))
          case Failure(e) => fail(formatThrowable(e))
        }
    }
}
