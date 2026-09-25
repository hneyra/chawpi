package chawpi.documents

import chawpi.automation.AutomationRunner
import chawpi.automation.AutomationService
import chawpi.automation.autoconfigure.ChawpiAutomationAutoConfiguration
import chawpi.documents.autoconfigure.ChawpiDocumentsAutoConfiguration
import chawpi.documents.autoconfigure.ChawpiDocumentsAutomationAutoConfiguration
import chawpi.test.ChawpiContextRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.context.annotation.ImportCandidates
import org.springframework.boot.test.context.FilteredClassLoader
import org.springframework.test.util.ReflectionTestUtils

class ChawpiDocumentsAutomationAutoConfigurationTest {
    @Test
    fun `with automation installed, a GENERATE_DOCUMENT action issues through documents`() {
        ChawpiContextRunner
            .core()
            .withConfiguration(
                AutoConfigurations.of(
                    ChawpiAutomationAutoConfiguration::class.java,
                    ChawpiDocumentsAutoConfiguration::class.java,
                    ChawpiDocumentsAutomationAutoConfiguration::class.java
                )
            ).withPropertyValues("chawpi.automation.poll-interval=0s")
            .run { context ->
                assertThat(context).hasNotFailed()
                val adapter = context.getBean(DocumentIssuerAdapter::class.java)
                assertThat(ReflectionTestUtils.getField(context.getBean(AutomationService::class.java), "documents")).isSameAs(adapter)
                assertThat(ReflectionTestUtils.getField(context.getBean(AutomationRunner::class.java), "documents")).isSameAs(adapter)
            }
    }

    // an app with documents and no automation: the adapter's class would not even load
    @Test
    fun `without automation on the classpath, documents boots alone`() {
        ChawpiContextRunner
            .core()
            .withClassLoader(FilteredClassLoader("chawpi.automation"))
            .withConfiguration(AutoConfigurations.of(ChawpiDocumentsAutoConfiguration::class.java, ChawpiDocumentsAutomationAutoConfiguration::class.java))
            .run { context ->
                assertThat(context).hasNotFailed()
                assertThat(context).hasSingleBean(DocumentService::class.java)
                assertThat(context).doesNotHaveBean("documentIssuerAdapter")
            }
    }

    @Test
    fun `with documents switched off, no adapter is left behind`() {
        ChawpiContextRunner
            .core()
            .withConfiguration(AutoConfigurations.of(ChawpiDocumentsAutoConfiguration::class.java, ChawpiDocumentsAutomationAutoConfiguration::class.java))
            .withPropertyValues("chawpi.documents.enabled=false")
            .run { context ->
                assertThat(context).hasNotFailed()
                assertThat(context).doesNotHaveBean("documentIssuerAdapter")
            }
    }

    @Test
    fun `the imports file registers both documents auto-configs`() {
        assertThat(ImportCandidates.load(AutoConfiguration::class.java, javaClass.classLoader).candidates)
            .contains(
                "chawpi.documents.autoconfigure.ChawpiDocumentsAutoConfiguration",
                "chawpi.documents.autoconfigure.ChawpiDocumentsAutomationAutoConfiguration"
            )
    }
}
