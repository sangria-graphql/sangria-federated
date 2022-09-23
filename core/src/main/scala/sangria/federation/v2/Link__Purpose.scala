package sangria.federation.v2

import sangria.schema._

object Link__Purpose extends Enumeration {

  val SECURITY, EXECUTION = Value

  val Type: EnumType[Link__Purpose.Value] = EnumType(
    name = "link__Purpose",
    values = List(
      EnumValue(
        name = "SECURITY",
        description =
          Some("`SECURITY` features provide metadata necessary to securely resolve fields."),
        value = SECURITY),
      EnumValue(
        name = "EXECUTION",
        description =
          Some("`EXECUTION` features provide metadata necessary for operation execution."),
        value = EXECUTION)
    )
  )
}
