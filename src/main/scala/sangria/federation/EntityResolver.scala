package sangria.federation

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
  )(implicit ev: Decoder[Node, A]) = new EntityResolver[Ctx, Node] {

    type Arg = A

    val decoder = ev

    def typename = __typeName
    def resolve(arg: Arg): LeafAction[Ctx, Option[Val]] = resolver(arg)
  }
}
