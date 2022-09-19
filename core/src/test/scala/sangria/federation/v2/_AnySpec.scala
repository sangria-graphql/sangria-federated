package sangria.federation.v2

import io.circe._
import io.circe.parser._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.EitherValues
import sangria.marshalling.InputUnmarshaller
import sangria.validation.Violation

class _AnySpec extends AnyWordSpec with Matchers with EitherValues {

  implicit private val um: InputUnmarshaller[Json] =
    Federation.upgrade(sangria.marshalling.circe.CirceInputUnmarshaller)

  "_Any scalar coercion accepts an object with __typename field" in {
    // https://www.apollographql.com/docs/federation/federation-spec/#scalar-_any
    parseUserInput(
      parse("""{ "__typename": "foo", "foo": "bar" }""")
        .getOrElse(Json.Null)).value shouldBe a[_Any[_]]
  }

  def parseUserInput(value: Json): Either[Violation, _Any[Json]] =
    _Any.__type.coerceUserInput(um.getScalarValue(value))
}
