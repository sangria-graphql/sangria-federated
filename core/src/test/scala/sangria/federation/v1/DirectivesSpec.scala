package sangria.federation.v1

import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import sangria.federation.renderLike

class DirectivesSpec extends AnyWordSpec {
  "directives for federation v2" should {
    "support @key directive" in {
      // https://www.apollographql.com/docs/federation/federated-types/federated-directives#key
      Directives.Key("id") must renderLike("""@key(fields: "id")""")
    }

    "support @extends directive" in {
      // https://www.apollographql.com/docs/federation/federated-types/federated-directives#extends
      Directives.Extends must renderLike("@extends")
    }

    "support @external directive" in {
      // https://www.apollographql.com/docs/federation/federated-types/federated-directives#external
      Directives.External must renderLike("@external")
    }

    "support @provides directive" in {
      // https://www.apollographql.com/docs/federation/federated-types/federated-directives#provides
      Directives.Provides(fields = "name") must renderLike("""@provides(fields: "name")""")
    }

    "support @requires directive" in {
      // https://www.apollographql.com/docs/federation/federated-types/federated-directives#requires
      Directives.Requires(fields = "size weight") must
        renderLike("""@requires(fields: "size weight")""")
    }
  }
}
