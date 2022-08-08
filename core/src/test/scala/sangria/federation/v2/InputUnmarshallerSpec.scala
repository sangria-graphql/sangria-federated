package sangria.federation.v2

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

    "A Scalar that is an Object without '__typename' but with a 'name' field" when {
      "the 'name' is a non-string, the default getScalarValue should be called" in {
        val default = stub[InputUnmarshaller[Unit]]
        (default.isMapNode _).when(()).returns(true)
        (default.getMapValue _).when((), "__typename").returns(None)
        (default.getMapValue _).when((), "name").returning(Some(()))
        (default.getScalaScalarValue _).when(()).returns(())

        val upgrade: InputUnmarshaller[Unit] = Federation.upgrade(default)
        upgrade.getScalarValue(())

        (default.getScalarValue _).verify(())
      }
      "The 'name' is a String, the Scalar should be a Link__Import__Object" in {
        val default = stub[InputUnmarshaller[Unit]]
        (default.isMapNode _).when(()).returns(true)
        (default.getMapValue _).when((), "__typename").returns(None)
        (default.getMapValue _).when((), "name").returning(Some(()))
        (default.getScalaScalarValue _).when(()).returns("myname")
        (default.getMapValue _).when((), "as").returns(None)

        val upgrade: InputUnmarshaller[Unit] = Federation.upgrade(default)

        upgrade.getScalarValue(()) should be(Link__Import_Object("myname"))
      }
    }

    "The Scalar is an Object without __typename but with a string 'name' field and an 'as' field" when {
      "The 'as' is a non-string, the Scalar should be a Link__Inport__Object with only name" in {
        val default = stub[InputUnmarshaller[Unit]]
        (default.isMapNode _).when(()).returns(true)
        (default.getMapValue _).when((), "__typename").returns(None)
        (default.getMapValue _).when((), "name").returning(Some(()))
        (default.getScalaScalarValue _).when(()).once().returns("myname")
        (default.getMapValue _).when((), "as").returns(Some(()))
        (default.getScalaScalarValue _).when(()).once().returns(())

        val upgrade: InputUnmarshaller[Unit] = Federation.upgrade(default)

        upgrade.getScalarValue(()) should be(Link__Import_Object("myname"))
      }
      "The 'as' is a String, the Scalar should be a Link__Inport__Object with name and as" in {
        val default = stub[InputUnmarshaller[Unit]]
        (default.isMapNode _).when(()).returns(true)
        (default.getMapValue _).when((), "__typename").returns(None)
        (default.getMapValue _).when((), "name").returning(Some(()))
        (default.getScalaScalarValue _).when(()).once().returns("myname")
        (default.getMapValue _).when((), "as").returns(Some(()))
        (default.getScalaScalarValue _).when(()).once().returns("myas")

        val upgrade: InputUnmarshaller[Unit] = Federation.upgrade(default)

        upgrade.getScalarValue(()) should be(Link__Import_Object("myname", Some("myas")))
      }
    }
  }
}
