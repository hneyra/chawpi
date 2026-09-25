package chawpi.core.data

import chawpi.core.metadata.CustomField
import chawpi.core.metadata.CustomObject
import chawpi.core.metadata.FieldType
import chawpi.core.metadata.ObjectDefinition
import java.util.UUID

object ObjectDefinitionFixtures {
    val obj = CustomObject(UUID.randomUUID(), UUID.randomUUID(), "predio", "Predio", "Predios", null, true, "predio__1234abcd", null, null)

    fun empty(): ObjectDefinition = ObjectDefinition(obj, emptyList())

    fun field(
        name: String,
        type: FieldType
    ) = CustomField(UUID.randomUUID(), obj.id, name, name, type, name, false, false, null, null, 0, null, null, emptyMap(), true, true)
}
