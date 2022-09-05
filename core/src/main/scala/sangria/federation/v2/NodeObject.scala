package sangria.federation.v2

private[federation] trait NodeObject[Node] {

  def __typename: Option[String]
  def decode[T](implicit ev: Decoder[Node, T]): Either[Exception, T]
}
