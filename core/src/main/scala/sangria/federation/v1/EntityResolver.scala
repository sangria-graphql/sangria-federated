package sangria.federation.v1

import sangria.schema._

trait EntityResolver[Ctx, Node] {

  type Arg

  val decoder: Decoder[Node, Arg]

  def typename: String
  def resolve(arg: Arg): LeafAction[Ctx, Option[_]]
}

object EntityResolver {

  def apply[Ctx, Node, Val, A](
      __typeName: String,
      resolver: A => LeafAction[Ctx, Option[Val]]
  )(implicit ev: Decoder[Node, A]): EntityResolver[Ctx, Node] {
    type Arg = A
  } = new EntityResolver[Ctx, Node] {

    type Arg = A

    val decoder: Decoder[Node, A] = ev

    def typename: String = __typeName
    def resolve(arg: Arg): LeafAction[Ctx, Option[Val]] = resolver(arg)
  }
}
