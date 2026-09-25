package chawpi.gis

import chawpi.forms.autoconfigure.ChawpiFormsAutoConfiguration
import chawpi.gis.autoconfigure.ChawpiGisAutoConfiguration
import chawpi.gis.autoconfigure.ChawpiGisPagesAutoConfiguration
import chawpi.pages.ComponentType
import chawpi.pages.PageComponentTypes
import chawpi.pages.autoconfigure.ChawpiPagesAutoConfiguration
import chawpi.test.ChawpiContextRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.context.annotation.ImportCandidates
import org.springframework.boot.test.context.FilteredClassLoader

class ChawpiGisPagesAutoConfigurationTest {
    private val all =
        AutoConfigurations.of(
            ChawpiFormsAutoConfiguration::class.java,
            ChawpiPagesAutoConfiguration::class.java,
            ChawpiGisAutoConfiguration::class.java,
            ChawpiGisPagesAutoConfiguration::class.java
        )

    @Test
    fun `with pages installed, MAP is a page component`() {
        ChawpiContextRunner.core().withConfiguration(all).run { context ->
            assertThat(context).hasNotFailed()
            assertThat(context.getBean(PageComponentTypes::class.java).provider(ComponentType("MAP"))).isInstanceOf(MapPageComponent::class.java)
        }
    }

    @Test
    fun `with gis switched off, pages has no MAP`() {
        ChawpiContextRunner.core().withConfiguration(all).withPropertyValues("chawpi.gis.enabled=false").run { context ->
            assertThat(context).hasNotFailed()
            assertThat(context.getBean(PageComponentTypes::class.java).provider(ComponentType("MAP"))).isNull()
        }
    }

    @Test
    fun `without pages on the classpath, gis boots alone`() {
        ChawpiContextRunner
            .core()
            .withClassLoader(FilteredClassLoader("chawpi.pages"))
            .withConfiguration(AutoConfigurations.of(ChawpiGisAutoConfiguration::class.java, ChawpiGisPagesAutoConfiguration::class.java))
            .run { context ->
                assertThat(context).hasNotFailed()
                assertThat(context).hasSingleBean(FeatureController::class.java)
                assertThat(context).doesNotHaveBean("mapPageComponent")
            }
    }

    @Test
    fun `the imports file registers both gis auto-configs`() {
        assertThat(ImportCandidates.load(AutoConfiguration::class.java, javaClass.classLoader).candidates)
            .contains("chawpi.gis.autoconfigure.ChawpiGisAutoConfiguration", "chawpi.gis.autoconfigure.ChawpiGisPagesAutoConfiguration")
    }
}
