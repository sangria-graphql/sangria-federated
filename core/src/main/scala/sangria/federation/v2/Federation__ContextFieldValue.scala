package sangria.federation.v2

import sangria.schema.{ScalarType, StringType}

/** [federation__ContextFieldValue](https://www.apollographql.com/docs/graphos/reference/federation/directives#fromcontext)
  * scalar, used by the [[Directives.FromContext @fromContext]] directive
  */
object Federation__ContextFieldValue {

  val Type: ScalarType[String] =
    StringType.rename("federation__ContextFieldValue").copy(description = None)
}
