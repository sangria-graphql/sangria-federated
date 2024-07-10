package sangria.federation.v2

import sangria.schema._

trait EntityResolver[Ctx, Node] {

  type Arg

  val decoder: Decoder[Node, Arg]

  def typename: String
  def resolve(arg: Arg)(ctx: Context[Ctx, _]): LeafAction[Ctx, Option[_]]
}

object EntityResolver {

  def apply[Ctx, Node, Val, A](
      __typeName: String,
      resolver: A => Context[Ctx, Val] => LeafAction[Ctx, Option[Val]]
  )(implicit ev: Decoder[Node, A]): EntityResolver[Ctx, Node] {
    type Arg = A
  } = new EntityResolver[Ctx, Node] {

    type Arg = A

    val decoder: Decoder[Node, A] = ev

    def typename: String = __typeName
    def resolve(arg: Arg)(ctx: Context[Ctx, _]): LeafAction[Ctx, Option[Val]] =
      resolver(arg)(ctx.asInstanceOf[Context[Ctx, Val]])
  }
}
