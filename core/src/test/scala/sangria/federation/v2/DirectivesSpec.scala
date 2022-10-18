package sangria.federation.v2

import org.scalatest.matchers.must.Matchers._
import org.scalatest.wordspec.AnyWordSpec
import sangria.federation.renderLike

class DirectivesSpec extends AnyWordSpec {
  "directives for federation v2" should {
    "support @link directive" in {
      // https://www.apollographql.com/docs/federation/federated-types/federated-directives#the-link-directive
      // https://specs.apollo.dev/link/v1.0/#@link
      Directives.Link("https://example.com/otherSchema") must renderLike(
        """@link(url: "https://example.com/otherSchema")""")
      Directives.Link("https://example.com/otherSchema", as = "eg") must renderLike(
        """@link(url: "https://example.com/otherSchema", as: "eg")""")
      Directives.Link(
        "https://example.com/otherSchema",
        `import` = Some(Vector(Link__Import("@link"), Link__Import("Purpose")))) must renderLike(
        """@link(url: "https://example.com/otherSchema", import: ["@link", "Purpose"])""")
      Directives.Link(
        "https://example.com/otherSchema",
        `import` = Some(Vector(Link__Import_Object("@example"), Link__Import_Object("Purpose")))
      ) must renderLike(
        """@link(url: "https://example.com/otherSchema", import: [{name: "@example"}, {name: "Purpose"}])""")
      Directives.Link(
        "https://example.com/otherSchema",
        `import` = Some(
          Vector(
            Link__Import_Object("@example", Some("@eg")),
            Link__Import_Object("Purpose", Some("LinkPurpose"))))
      ) must renderLike(
        """@link(url: "https://example.com/otherSchema", import: [{name: "@example", as: "@eg"}, {name: "Purpose", as: "LinkPurpose"}])""")
      Directives.Link("https://example.com/otherSchema", `for` = Link__Purpose.EXECUTION) must
        renderLike("""@link(url: "https://example.com/otherSchema", for: EXECUTION)""")
      Directives.Link("https://example.com/otherSchema", `for` = Link__Purpose.SECURITY) must
        renderLike("""@link(url: "https://example.com/otherSchema", for: SECURITY)""")
    }

    "support @key directive" in {
      // https://www.apollographql.com/docs/federation/federated-types/federated-directives#key
      Directives.Key("id") must renderLike("""@key(fields: "id")""")
      Directives.Key("id", resolvable = true) must
        renderLike("""@key(fields: "id", resolvable: true)""")
      Directives.Key("id", resolvable = false) must
        renderLike("""@key(fields: "id", resolvable: false)""")
    }

    "support @extends directive" in {
      // https://www.apollographql.com/docs/federation/federated-types/federated-directives#extends
      Directives.Extends must renderLike("@extends")
    }

    "support @shareable directive" in {
      // https://www.apollographql.com/docs/federation/federated-types/federated-directives#shareable
      Directives.Shareable must renderLike("@shareable")
    }

    "support @inaccessible directive" in {
      // https://www.apollographql.com/docs/federation/federated-types/federated-directives#inaccessible
      Directives.Inaccessible must renderLike("@inaccessible")
    }

    "support @override directive" in {
      // https://www.apollographql.com/docs/federation/federated-types/federated-directives#override
      Directives.Override(from = "Products") must renderLike("""@override(from: "Products")""")
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

    "support @tag directive" in {
      // https://www.apollographql.com/docs/federation/federated-types/federated-directives#tag
      Directives.Tag(name = "team-admin") must renderLike("""@tag(name: "team-admin")""")
    }
  }
}
