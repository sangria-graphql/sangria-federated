package sangria.federation.v2

import sangria.schema.{ScalarType, StringType}

/** [federation__Scope](https://www.apollographql.com/docs/graphos/reference/federation/directives#requiresscopes)
  * scalar, used by the [[Directives.RequiresScopes @requiresScopes]] directive
  */
object Federation__Scope {

  val Type: ScalarType[String] = StringType.rename("federation__Scope").copy(description = None)
}
