package sangria.federation

import sangria.schema._

case class _Service(sdl: String)

object _Service {

  val Type = ObjectType(
    name = "_Service",
    fields[Unit, _Service](Field("sdl", StringType, resolve = _.value.sdl)))
}
