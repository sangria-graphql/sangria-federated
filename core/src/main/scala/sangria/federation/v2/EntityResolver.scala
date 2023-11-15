package sangria.federation.v2

import sangria.schema._

trait EntityResolver[Ctx, Node] {

  type Arg
  type Val

  val decoder: Decoder[Node, Arg]

  def typename: String
  def resolve(arg: Seq[Arg], ctx: Context[Ctx, Val]): LeafAction[Ctx, Iterable[Val]]
  def arg(v: Val): Arg
}

object EntityResolver {
  def apply[Ctx, Node, Value, Id](
      __typeName: String,
      resolver: (Seq[Id], Context[Ctx, Value]) => LeafAction[Ctx, Iterable[Value]],
      id: Value => Id
  )(implicit ev: Decoder[Node, Id]): EntityResolver[Ctx, Node] { type Arg = Id; type Val = Value } =
    new EntityResolver[Ctx, Node] {

      override type Arg = Id
      override type Val = Value

      override val decoder: Decoder[Node, Id] = ev

      override def typename: String = __typeName

      override def resolve(arg: Seq[Arg], ctx: Context[Ctx, Val]): LeafAction[Ctx, Iterable[Val]] =
        resolver(arg, ctx)

      override def arg(v: Value): Id = id(v)
    }
}
