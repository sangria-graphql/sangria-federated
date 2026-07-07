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

    "support @interfaceObject directive" in {
      // https://www.apollographql.com/docs/federation/federated-types/federated-directives#interfaceobject
      Directives.InterfaceObject must renderLike("@interfaceObject")
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

    "support @authenticated directive" in {
      // https://www.apollographql.com/docs/graphos/reference/federation/directives#authenticated
      Directives.Authenticated must renderLike("@authenticated")
    }

    "support @requiresScopes directive" in {
      // https://www.apollographql.com/docs/graphos/reference/federation/directives#requiresscopes
      Directives.RequiresScopes(Vector(Vector("scope1", "scope2"), Vector("scope3"))) must
        renderLike("""@requiresScopes(scopes: [["scope1", "scope2"], ["scope3"]])""")
    }

    "support @policy directive" in {
      // https://www.apollographql.com/docs/graphos/reference/federation/directives#policy
      Directives.Policy(Vector(Vector("policy1", "policy2"), Vector("policy3"))) must
        renderLike("""@policy(policies: [["policy1", "policy2"], ["policy3"]])""")
    }

    "support @context directive" in {
      // https://www.apollographql.com/docs/graphos/reference/federation/directives#context
      Directives.Context(name = "userContext") must renderLike("""@context(name: "userContext")""")
    }

    "support @fromContext directive" in {
      // https://www.apollographql.com/docs/graphos/reference/federation/directives#fromcontext
      Directives.FromContext(field = "$userContext { prop }") must
        renderLike("""@fromContext(field: "$userContext { prop }")""")
    }

    "support @cost directive" in {
      // https://www.apollographql.com/docs/graphos/reference/federation/directives#cost
      Directives.Cost(weight = 5) must renderLike("""@cost(weight: 5)""")
    }

    "support @listSize directive" in {
      // https://www.apollographql.com/docs/graphos/reference/federation/directives#listsize
      Directives.ListSize() must renderLike("@listSize")
      Directives.ListSize(assumedSize = Some(10)) must
        renderLike("""@listSize(assumedSize: 10)""")
      Directives.ListSize(slicingArguments = Some(Vector("first", "last"))) must
        renderLike("""@listSize(slicingArguments: ["first", "last"])""")
      Directives.ListSize(sizedFields = Some(Vector("page"))) must
        renderLike("""@listSize(sizedFields: ["page"])""")
      Directives.ListSize(requireOneSlicingArgument = Some(false)) must
        renderLike("""@listSize(requireOneSlicingArgument: false)""")
      Directives.ListSize(
        assumedSize = Some(10),
        slicingArguments = Some(Vector("first", "last")),
        sizedFields = Some(Vector("page")),
        requireOneSlicingArgument = Some(true)
      ) must renderLike(
        """@listSize(assumedSize: 10, slicingArguments: ["first", "last"], sizedFields: ["page"], requireOneSlicingArgument: true)""")
    }
  }
}
