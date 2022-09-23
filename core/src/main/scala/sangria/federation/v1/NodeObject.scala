package sangria.federation.v1

import scala.annotation.unused

private[federation] trait NodeObject[Node] {

  def __typename: Option[String]
  def decode[T](implicit @unused ev: Decoder[Node, T]): Either[Exception, T]
}
