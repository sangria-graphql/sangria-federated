package sangria.federation

import org.scalamock.scalatest.MockFactory
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import sangria.marshalling.InputUnmarshaller

class InputUnmarshallerSpec extends AnyFreeSpec with Matchers with MockFactory {

  "Federated Upgraded InputUnmarshaller" - {
    "MapNode Scalar" - {
      "with __typeName is NodeObject" in {
        val default = stub[InputUnmarshaller[Unit]]
        (default.isMapNode _).when(()).returns(true)
        (default.getMapValue _).when((), "__typename").returns(Some(()))

        val upgrade: InputUnmarshaller[Unit] = Federation.upgrade(default)

        upgrade.getScalarValue(()) shouldBe a[NodeObject[Unit]]
      }
      "without __typename" - {
        "with a non-string name calls default getScalarValue" in {
          val default = stub[InputUnmarshaller[Unit]]
          (default.isMapNode _).when(()).returns(true)
          (default.getMapValue _).when((), "__typename").returns(None)
          (default.getMapValue _).when((), "name").returning(Some(()))
          (default.getScalaScalarValue _).when(()).returns(())

          val upgrade: InputUnmarshaller[Unit] = Federation.upgrade(default)
          upgrade.getScalarValue(())

          (default.getScalarValue _).verify(())
        }
        "with a string name" - {
          "without as should be a Link__Import_Object with only name" in {
            val default = stub[InputUnmarshaller[Unit]]
            (default.isMapNode _).when(()).returns(true)
            (default.getMapValue _).when((), "__typename").returns(None)
            (default.getMapValue _).when((), "name").returning(Some(()))
            (default.getScalaScalarValue _).when(()).returns("myname")
            (default.getMapValue _).when((), "as").returns(None)

            val upgrade: InputUnmarshaller[Unit] = Federation.upgrade(default)

            upgrade.getScalarValue(()) should be(Link__Import_Object("myname"))
          }
          "with as" - {
            "if as is a non-string it should be a Link__Import_Object with only name" in {
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
            "if as is a string it should be a Link__Import_Object with name and as" in {
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
    }
    "Not-MapNode calls default getScalarValue" in {
      val default = stub[InputUnmarshaller[Unit]]
      (default.isMapNode _).when(()).returns(false)

      val upgrade: InputUnmarshaller[Unit] = Federation.upgrade(default)
      upgrade.getScalarValue(())

      (default.getScalarValue _).verify(())
    }
  }
}
