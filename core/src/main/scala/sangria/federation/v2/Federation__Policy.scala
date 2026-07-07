package sangria.federation.v2

import sangria.schema.{ScalarType, StringType}

/** [federation__Policy](https://www.apollographql.com/docs/graphos/reference/federation/directives#policy)
  * scalar, used by the [[Directives.Policy @policy]] directive
  */
object Federation__Policy {

  val Type: ScalarType[String] = StringType.rename("federation__Policy").copy(description = None)
}
