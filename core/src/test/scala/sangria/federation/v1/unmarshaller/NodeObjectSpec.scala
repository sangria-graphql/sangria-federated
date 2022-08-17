package sangria.federation.v1.unmarshaller

import org.scalamock.scalatest.MockFactory
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import sangria.federation.v1.{Federation, NodeObject}
import sangria.marshalling.InputUnmarshaller

class NodeObjectSpec extends AnyWordSpec with Matchers with MockFactory {

  "A Scalar Object with a __typeName field should be a NodeObject" in {
    val default = stub[InputUnmarshaller[Unit]]
    (default.isMapNode _).when(()).returns(true)
    (default.getMapValue _).when((), "__typename").returns(Some(()))

    val upgrade: InputUnmarshaller[Unit] = Federation.upgrade(default)

    upgrade.getScalarValue(()) shouldBe a[NodeObject[Unit]]
  }
}
