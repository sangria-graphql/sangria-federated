package sangria.federation.v2

import sangria.ast._

object Query {

  val _service: FieldDefinition = FieldDefinition(
    name = "_service",
    fieldType = NotNullType(NamedType("_Service")),
    arguments = Vector.empty)

  val _entities: FieldDefinition =
    FieldDefinition(
      name = "_entities",
      fieldType = NotNullType(ListType(NamedType("_Entity"))),
      arguments = Vector(
        InputValueDefinition(
          name = "representations",
          valueType = NotNullType(ListType(NotNullType(NamedType("_Any")))),
          defaultValue = None))
    )

  def queryType(fields: FieldDefinition*): ObjectTypeExtensionDefinition =
    ObjectTypeExtensionDefinition(
      name = "Query",
      interfaces = Vector.empty,
      fields = fields.toVector)
}
