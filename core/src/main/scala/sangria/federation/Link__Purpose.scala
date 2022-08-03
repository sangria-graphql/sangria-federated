package sangria.federation

import sangria.schema.{ EnumValue, EnumType }

object Link__Purpose extends Enumeration {

  val SECURITY, EXECUTION = Value

  val Type = EnumType(
    name = "link__Purpose",
    values = List(
      EnumValue("SECURITY",
        value = SECURITY,
        description = Some("`SECURITY` features provide metadata necessary to securely resolve fields.")),
      EnumValue("SECURITY",
        value = EXECUTION,
        description = Some("`EXECUTION` features provide metadata necessary for operation execution.")),
    )
  )
}
