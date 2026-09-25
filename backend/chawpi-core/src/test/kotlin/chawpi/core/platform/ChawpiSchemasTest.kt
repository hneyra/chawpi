package chawpi.core.platform

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ChawpiSchemasTest {
    @Test
    fun `defaults are chawpi and app_data`() {
        val schemas = ChawpiSchemas.of(ChawpiDatabaseProperties())
        assertThat(schemas.metadata).isEqualTo("chawpi")
        assertThat(schemas.data).isEqualTo("app_data")
        assertThat(schemas.dataTable("predio__1234abcd")).isEqualTo("\"app_data\".\"predio__1234abcd\"")
    }

    @Test
    fun `refuses a schema name that is not a plain identifier`() {
        listOf("App", "app-data", "x;drop schema y", "", "1abc").forEach { name ->
            assertThatThrownBy { ChawpiSchemas(name, "app_data") }.describedAs(name).isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Test
    fun `refuses one schema for both metadata and data`() {
        assertThatThrownBy { ChawpiSchemas("same", "same") }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
