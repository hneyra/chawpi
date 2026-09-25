package chawpi.views

import chawpi.core.platform.ModuleMigration
import chawpi.test.ChawpiContextRunner
import chawpi.views.autoconfigure.ChawpiViewsAutoConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.context.annotation.ImportCandidates

class ChawpiViewsAutoConfigurationTest {
    private val runner = ChawpiContextRunner.core().withConfiguration(AutoConfigurations.of(ChawpiViewsAutoConfiguration::class.java))

    @Test
    fun `views wires on core and brings its migration`() {
        runner.run { context ->
            assertThat(context).hasNotFailed()
            assertThat(context).hasSingleBean(ViewService::class.java)
            assertThat(context).hasSingleBean(ViewController::class.java)
            assertThat(context).hasSingleBean(ViewMetadataController::class.java)
            assertThat(context.getBeansOfType(ModuleMigration::class.java).values.map { it.name }).containsExactlyInAnyOrder("core", "views")
        }
    }

    @Test
    fun `switched off, core boots without any views bean`() {
        runner.withPropertyValues("chawpi.views.enabled=false").run { context ->
            assertThat(context).hasNotFailed()
            assertThat(context).doesNotHaveBean(ViewService::class.java)
            assertThat(context).doesNotHaveBean(ViewMetadataController::class.java)
            assertThat(context.getBeansOfType(ModuleMigration::class.java).values.map { it.name }).containsExactly("core")
        }
    }

    @Test
    fun `an app's own service wins`() {
        val mine = mock(ViewService::class.java)
        runner.withBean(ViewService::class.java, { mine }).run { context ->
            assertThat(context.getBean(ViewService::class.java)).isSameAs(mine)
        }
    }

    @Test
    fun `the imports file registers the auto-config`() {
        assertThat(ImportCandidates.load(AutoConfiguration::class.java, javaClass.classLoader).candidates)
            .contains("chawpi.views.autoconfigure.ChawpiViewsAutoConfiguration")
    }
}
