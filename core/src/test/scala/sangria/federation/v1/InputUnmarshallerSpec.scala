package sangria.federation.v1

import org.scalamock.scalatest.MockFactory
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import sangria.marshalling.InputUnmarshaller

class InputUnmarshallerSpec extends AnyWordSpec with Matchers with MockFactory {

  "A Scalar" when {
    "The Scalar is not an Object should call the default getScalarValue" in {
      val default = stub[InputUnmarshaller[Unit]]
      (default.isMapNode _).when(()).returns(false)

      val upgrade: InputUnmarshaller[Unit] = Federation.upgrade(default)
      upgrade.getScalarValue(())

      (default.getScalarValue _).verify(())
    }

    "The Scalar is an Object with a __typeName field should be a NodeObject" in {
      val default = stub[InputUnmarshaller[Unit]]
      (default.isMapNode _).when(()).returns(true)
      (default.getMapValue _).when((), "__typename").returns(Some(()))

      val upgrade: InputUnmarshaller[Unit] = Federation.upgrade(default)

      upgrade.getScalarValue(()) shouldBe a[NodeObject[Unit]]
    }
  }
}
