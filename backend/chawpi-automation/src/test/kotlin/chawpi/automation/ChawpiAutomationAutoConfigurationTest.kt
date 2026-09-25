package chawpi.automation

import chawpi.automation.autoconfigure.ChawpiAutomationAutoConfiguration
import chawpi.core.data.RecordChangeListener
import chawpi.core.metadata.FieldUsage
import chawpi.core.platform.ModuleMigration
import chawpi.test.ChawpiContextRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.context.annotation.ImportCandidates
import org.springframework.test.util.ReflectionTestUtils
import java.util.UUID

class ChawpiAutomationAutoConfigurationTest {
    // poll-interval 0: no background drain against the mocked database
    private val runner =
        ChawpiContextRunner
            .core()
            .withConfiguration(AutoConfigurations.of(ChawpiAutomationAutoConfiguration::class.java))
            .withPropertyValues("chawpi.automation.poll-interval=0s")

    private val neighbour =
        object : DocumentIssuer {
            override suspend fun typeExists(
                objectId: UUID,
                name: String
            ) = true

            override suspend fun issue(
                organizationId: UUID,
                objectName: String,
                recordId: UUID,
                typeName: String
            ) = "SGTM-2026-001"
        }

    @Test
    fun `automation joins core's listeners and field usages`() {
        runner.run { context ->
            assertThat(context).hasNotFailed()
            assertThat(context).hasSingleBean(AutomationService::class.java)
            assertThat(context).hasSingleBean(AutomationController::class.java)
            assertThat(context.getBeansOfType(RecordChangeListener::class.java).values).hasAtLeastOneElementOfType(AutomationDispatcher::class.java)
            assertThat(context.getBeansOfType(FieldUsage::class.java).values).hasAtLeastOneElementOfType(AutomationFieldUsage::class.java)
            assertThat(context.getBeansOfType(ModuleMigration::class.java).values.map { it.name }).containsExactlyInAnyOrder("core", "automation")
        }
    }

    @Test
    fun `without a documents module it falls back to the null issuer`() {
        runner.run { context ->
            assertThat(ReflectionTestUtils.getField(context.getBean(AutomationService::class.java), "documents")).isInstanceOf(NoDocumentIssuer::class.java)
            assertThat(ReflectionTestUtils.getField(context.getBean(AutomationRunner::class.java), "documents")).isInstanceOf(NoDocumentIssuer::class.java)
        }
    }

    @Test
    fun `with a document issuer next to it, that one is used`() {
        runner.withBean(DocumentIssuer::class.java, { neighbour }).run { context ->
            assertThat(ReflectionTestUtils.getField(context.getBean(AutomationService::class.java), "documents")).isSameAs(neighbour)
            assertThat(ReflectionTestUtils.getField(context.getBean(AutomationRunner::class.java), "documents")).isSameAs(neighbour)
        }
    }

    @Test
    fun `settings bind under chawpi automation`() {
        runner.withPropertyValues("chawpi.automation.allow-private-webhooks=true", "chawpi.automation.max-depth=5").run { context ->
            val properties = context.getBean(AutomationProperties::class.java)
            assertThat(properties.polling).isFalse()
            assertThat(properties.allowPrivateWebhooks).isTrue()
            assertThat(properties.depthCap).isEqualTo(5)
        }
    }

    @Test
    fun `switched off, nothing listens and nothing drains`() {
        runner.withPropertyValues("chawpi.automation.enabled=false").run { context ->
            assertThat(context).hasNotFailed()
            assertThat(context).doesNotHaveBean(AutomationService::class.java)
            assertThat(context).doesNotHaveBean(AutomationDrain::class.java)
            assertThat(context.getBeansOfType(RecordChangeListener::class.java)).isEmpty()
        }
    }

    @Test
    fun `the imports file registers the auto-config`() {
        assertThat(ImportCandidates.load(AutoConfiguration::class.java, javaClass.classLoader).candidates)
            .contains("chawpi.automation.autoconfigure.ChawpiAutomationAutoConfiguration")
    }
}
