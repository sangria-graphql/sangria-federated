package state

import io.circe.Json
import io.circe.generic.semiauto.deriveDecoder
import sangria.execution.deferred.{Fetcher, HasId}
import sangria.federation.v2.{Decoder, Directives, EntityResolver}
import sangria.schema.{
  Argument,
  Field,
  IntType,
  ListInputType,
  ListType,
  ObjectType,
  StringType,
  fields
}

object StateGraphQLSchema {
  val StateType: ObjectType[Unit, State] =
    ObjectType(
      "State",
      fields[Unit, State](
        Field("id", IntType, resolve = _.value.id),
        Field("key", StringType, resolve = _.value.key),
        Field("value", StringType, resolve = _.value.value))
    ).withDirective(Directives.Key("id"))

  implicit val stateId: HasId[State, Int] = _.id
  val states: Fetcher[StateContext, State, State, Int] = Fetcher {
    (ctx: StateContext, ids: Seq[Int]) =>
      ctx.state.getStates(ids)(ctx.ec)
  }

  private val ids = Argument("ids", ListInputType(IntType))
  val Query: ObjectType[StateContext, Any] = ObjectType(
    "Query",
    fields[StateContext, Any](
      Field(
        name = "states",
        fieldType = ListType(StateGraphQLSchema.StateType),
        arguments = List(ids),
        resolve = ctx => states.deferSeqOpt(ctx.arg(ids))))
  )

  // for GraphQL federation
  case class StateArg(id: Int)
  implicit val decoder: Decoder[Json, StateArg] = deriveDecoder[StateArg].decodeJson(_)
  val stateResolver = EntityResolver[StateContext, Json, State, StateArg](
    __typeName = StateType.name,
    arg => _ => states.deferOpt(arg.id))
}
