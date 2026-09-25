package chawpi.automation

import chawpi.core.common.ValidationException
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class NoDocumentIssuerTest {
    @Test
    fun `without a documents module no document type exists, so saving the action is refused`() =
        runTest {
            assertThat(NoDocumentIssuer().typeExists(UUID.randomUUID(), "permiso")).isFalse()
        }

    @Test
    fun `issuing without the module fails the run with a 400, not a crash`() =
        runTest {
            val error = runCatching { NoDocumentIssuer().issue(UUID.randomUUID(), "predio", UUID.randomUUID(), "permiso") }.exceptionOrNull()
            assertThat(error).isInstanceOf(ValidationException::class.java).hasMessage("Unknown document type 'permiso'")
        }
}
