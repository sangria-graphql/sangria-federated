package sangria.federation.v2

import org.scalamock.scalatest.MockFactory
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class Link__Import_Spec extends AnyWordSpec with Matchers with MockFactory with EitherValues {

  "Scalar Link__Import coercion (https://specs.apollo.dev/link/v1.0/#example-import-a-string-name)" should {
    "accept a string" in {
      Link__Import.Type.coerceUserInput("str").value shouldBe a[Link__Import]
    }
    "accept an object with 'name' and an optional 'as' string fields" in {
      Link__Import.Type
        .coerceUserInput(Link__Import_Object("str"))
        .value shouldBe a[Link__Import_Object]
    }
    "raise violation error in any other case" in {
      Link__Import.Type.coerceUserInput(()).left.value should be(
        Link__Import.Link__Import_Coercion_Violation)
    }
  }
}
