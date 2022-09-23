package sangria.federation.v1

import sangria.schema._

case class _Entity(__typename: String)

object _Entity {

  def apply[Ctx](types: List[ObjectType[Ctx, _]]): UnionType[Ctx] =
    UnionType[Ctx](name = "_Entity", types = types)
}
