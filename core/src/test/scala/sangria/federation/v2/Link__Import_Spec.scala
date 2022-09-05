package sangria.federation.v2

import io.circe._
import io.circe.parser._
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sangria.validation.Violation

class Link__Import_Spec extends AnyWordSpec with Matchers with EitherValues {

  implicit val um = Federation.upgrade(sangria.marshalling.circe.CirceInputUnmarshaller)

  "Scalar Link__Import coercion (https://specs.apollo.dev/link/v1.0/#example-import-a-string-name)" should {
    "accept a string" in {
      // https://specs.apollo.dev/link/v1.0/#example-import-a-string-name
      parseUserInput(Json.fromString("@link")).value shouldBe a[Link__Import]
    }
    "accept an object with a 'name' string field" in {
      // https://specs.apollo.dev/link/v1.0/#example-import-an-aliased-name
      parseUserInput(
        parse("""{ "name": "@example" }""").getOrElse(
          Json.Null)).value shouldBe Link__Import_Object(name = "@example")
    }
    "accept an object with 'name' and an optional 'as' string field" in {
      // https://specs.apollo.dev/link/v1.0/#example-import-an-aliased-name
      parseUserInput(
        parse("""{ "name": "@example", "as": "@eg" }""").getOrElse(
          Json.Null)).value shouldBe Link__Import_Object(name = "@example", as = Some("@eg"))
    }
    "raise exception if 'name' field is not a string" in {
      an[IllegalStateException] should be thrownBy parseUserInput(
        parse("""{ "name": 1 }""").getOrElse(Json.Null)).value
    }
    "raise violation error on any other scalar" in {
      parseUserInput(Json.fromInt(1)).left.value should be(
        Link__Import.Link__Import_Coercion_Violation)
    }
  }

  def parseUserInput(value: Json): Either[Violation, Abstract_Link__Import] =
    Link__Import.Type.coerceUserInput(um.getScalarValue(value))
}
