package chawpi.workflow

import chawpi.forms.autoconfigure.ChawpiFormsAutoConfiguration
import chawpi.pages.ComponentType
import chawpi.pages.PageComponentTypes
import chawpi.pages.autoconfigure.ChawpiPagesAutoConfiguration
import chawpi.test.ChawpiContextRunner
import chawpi.workflow.autoconfigure.ChawpiWorkflowAutoConfiguration
import chawpi.workflow.autoconfigure.ChawpiWorkflowPagesAutoConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.context.annotation.ImportCandidates
import org.springframework.boot.test.context.FilteredClassLoader

class ChawpiWorkflowPagesAutoConfigurationTest {
    private val all =
        AutoConfigurations.of(
            ChawpiFormsAutoConfiguration::class.java,
            ChawpiPagesAutoConfiguration::class.java,
            ChawpiWorkflowAutoConfiguration::class.java,
            ChawpiWorkflowPagesAutoConfiguration::class.java
        )

    @Test
    fun `with pages installed, WORKFLOW is a page component`() {
        ChawpiContextRunner.core().withConfiguration(all).run { context ->
            assertThat(context).hasNotFailed()
            assertThat(context.getBean(PageComponentTypes::class.java).provider(ComponentType("WORKFLOW")))
                .isInstanceOf(WorkflowPageComponent::class.java)
        }
    }

    @Test
    fun `with workflow switched off, pages has no WORKFLOW`() {
        ChawpiContextRunner.core().withConfiguration(all).withPropertyValues("chawpi.workflow.enabled=false").run { context ->
            assertThat(context).hasNotFailed()
            assertThat(context.getBean(PageComponentTypes::class.java).provider(ComponentType("WORKFLOW"))).isNull()
        }
    }

    @Test
    fun `without pages on the classpath, workflow boots alone`() {
        ChawpiContextRunner
            .core()
            .withClassLoader(FilteredClassLoader("chawpi.pages"))
            .withConfiguration(AutoConfigurations.of(ChawpiWorkflowAutoConfiguration::class.java, ChawpiWorkflowPagesAutoConfiguration::class.java))
            .run { context ->
                assertThat(context).hasNotFailed()
                assertThat(context).hasSingleBean(WorkflowService::class.java)
                assertThat(context).doesNotHaveBean("workflowPageComponent")
            }
    }

    @Test
    fun `the imports file registers both workflow auto-configs`() {
        assertThat(ImportCandidates.load(AutoConfiguration::class.java, javaClass.classLoader).candidates)
            .contains("chawpi.workflow.autoconfigure.ChawpiWorkflowAutoConfiguration", "chawpi.workflow.autoconfigure.ChawpiWorkflowPagesAutoConfiguration")
    }
}
