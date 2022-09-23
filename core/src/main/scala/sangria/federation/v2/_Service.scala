package sangria.federation.v2

import sangria.schema._

case class _Service(sdl: Option[String])

object _Service {

  val Type: ObjectType[Unit, _Service] = ObjectType(
    name = "_Service",
    fields[Unit, _Service](Field("sdl", OptionType(StringType), resolve = _.value.sdl)))
}
