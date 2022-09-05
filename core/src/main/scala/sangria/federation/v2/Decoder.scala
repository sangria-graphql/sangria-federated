package sangria.federation.v2

trait Decoder[Node, T] {

  def decode(node: Node): Either[Exception, T]
}
