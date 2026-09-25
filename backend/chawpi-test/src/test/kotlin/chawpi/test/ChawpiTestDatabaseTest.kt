package chawpi.test

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ChawpiTestDatabaseTest {
    @Test
    fun `refuses to wipe a database whose name does not end in _test`() {
        assertThatThrownBy { ChawpiTestDatabase.requireTestDatabaseName("chawpi") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("refusing to wipe 'chawpi'")
        assertThat(ChawpiTestDatabase.requireTestDatabaseName("chawpi_test")).isEqualTo("chawpi_test")
    }

    @Test
    fun `quotes identifiers it drops, doubling embedded quotes`() {
        assertThat(ChawpiTestDatabase.quoteIdentifier("app_data")).isEqualTo("\"app_data\"")
        assertThat(ChawpiTestDatabase.quoteIdentifier("we\"ird")).isEqualTo("\"we\"\"ird\"")
    }

    @Test
    fun `core tests run on plain postgres unless told otherwise`() {
        if (System.getProperty("chawpi.test.db.image") == null && System.getenv("CHAWPI_TEST_DB_IMAGE") == null) {
            assertThat(ChawpiTestDatabase.image).isEqualTo(ChawpiTestDatabase.DEFAULT_IMAGE)
        }
    }

    @Test
    fun `host set, port missing, refuses with a message naming the missing var`() {
        val env =
            mapOf(
                "CHAWPI_TEST_DB_HOST" to "db.internal",
                "CHAWPI_TEST_DB_NAME" to "chawpi_test",
                "CHAWPI_TEST_DB_USERNAME" to "chawpi",
                "CHAWPI_TEST_DB_PASSWORD" to "chawpi"
            )
        assertThatThrownBy { resolveExternalDatabaseConfig { env[it] } }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("CHAWPI_TEST_DB_PORT")
    }

    @Test
    fun `all five vars set, properties resolve to them`() {
        val env =
            mapOf(
                "CHAWPI_TEST_DB_HOST" to "db.internal",
                "CHAWPI_TEST_DB_PORT" to "5555",
                "CHAWPI_TEST_DB_NAME" to "chawpi_test",
                "CHAWPI_TEST_DB_USERNAME" to "chawpi",
                "CHAWPI_TEST_DB_PASSWORD" to "secret"
            )
        assertThat(resolveExternalDatabaseConfig { env[it] })
            .isEqualTo(
                mapOf(
                    "CHAWPI_TEST_DB_HOST" to "db.internal",
                    "CHAWPI_TEST_DB_PORT" to "5555",
                    "CHAWPI_TEST_DB_NAME" to "chawpi_test",
                    "CHAWPI_TEST_DB_USERNAME" to "chawpi",
                    "CHAWPI_TEST_DB_PASSWORD" to "secret"
                )
            )
    }
}
