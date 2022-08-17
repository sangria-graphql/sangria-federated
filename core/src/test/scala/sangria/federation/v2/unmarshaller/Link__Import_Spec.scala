package sangria.federation.v2.unmarshaller

import org.scalamock.scalatest.MockFactory
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import sangria.federation.v2.{Federation, Link__Import_Object}
import sangria.marshalling.InputUnmarshaller

class Link__Import_Spec extends AnyWordSpec with Matchers with MockFactory {

  "An Object Scalar" when {
    "the object contains a `name` field" should {
      "call the default getScalarValue, if the 'name' is a non-string" in {
        val default = stub[InputUnmarshaller[Unit]]
        (default.isMapNode _).when(()).returns(true)
        (default.getMapValue _).when((), "__typename").returns(None)
        (default.getMapValue _).when((), "name").returning(Some(()))
        (default.getScalaScalarValue _).when(()).returns(())

        val upgrade: InputUnmarshaller[Unit] = Federation.upgrade(default)
        upgrade.getScalarValue(())

        (default.getScalarValue _).verify(())
      }
      "be a Link__Import__Object, if the 'name' is a String" in {
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

    "the object contains a string 'name' field and an 'as' field" should {
      "be a Link__Import__Object with only name, if 'as' is a non-string" in {
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
      "be a Link__Import__Object with name and as, if 'as' is a String" in {
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
