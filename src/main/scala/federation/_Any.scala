package federation

case class _Any[Node](__typename: String, fields: NodeObject[Node])

object _Any {

  import sangria.schema.ScalarType
  import sangria.validation.ValueCoercionViolation

  case object AnyCoercionViolation extends ValueCoercionViolation("_Any value expected!!")

  case object TypeNameNotFound
      extends ValueCoercionViolation("__typename field is not defined in _Any value!!")

  def __type[Node] = ScalarType[_Any[Node]](
    name = "_Any",
    coerceOutput = { case _ =>
      "output"
    },
    coerceUserInput = {
      case n: NodeObject[Node] =>
        n.__typename match {
          case Some(__typename) => Right(_Any(__typename, n))
          case None => Left(TypeNameNotFound)
        }
      case _ => Left(AnyCoercionViolation)
    },
    coerceInput = { _ => Left(AnyCoercionViolation) }
  )
}
