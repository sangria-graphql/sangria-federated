package sangria.federation.v1

trait Decoder[Node, T] {

  def decode(node: Node): Either[Exception, T]
}
