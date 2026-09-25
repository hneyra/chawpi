package chawpi.test

import chawpi.core.data.RecordService
import chawpi.core.platform.ChawpiMigrations
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ChawpiContextRunnerTest {
    @Test
    fun `core wires with no database and runs no migration`() {
        ChawpiContextRunner.core().run { context ->
            assertThat(context).hasNotFailed()
            assertThat(context).hasSingleBean(RecordService::class.java)
            assertThat(context).doesNotHaveBean(ChawpiMigrations::class.java)
        }
    }
}
