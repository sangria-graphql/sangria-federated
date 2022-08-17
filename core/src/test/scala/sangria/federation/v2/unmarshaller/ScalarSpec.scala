package sangria.federation.v2.unmarshaller

import org.scalamock.scalatest.MockFactory
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import sangria.federation.v2.Federation
import sangria.marshalling.InputUnmarshaller

class ScalarSpec extends AnyWordSpec with Matchers with MockFactory {

  "A Scalar should call the default getScalarValue, if it's an object" in {
    val default = stub[InputUnmarshaller[Unit]]
    (default.isMapNode _).when(()).returns(false)

    val upgrade: InputUnmarshaller[Unit] = Federation.upgrade(default)
    upgrade.getScalarValue(())

    (default.getScalarValue _).verify(())
  }
}
