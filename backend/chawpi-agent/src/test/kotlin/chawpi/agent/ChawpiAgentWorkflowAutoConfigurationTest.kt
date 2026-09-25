package chawpi.agent

import chawpi.agent.autoconfigure.ChawpiAgentAutoConfiguration
import chawpi.agent.autoconfigure.ChawpiAgentWorkflowAutoConfiguration
import chawpi.test.ChawpiContextRunner
import chawpi.workflow.autoconfigure.ChawpiWorkflowAutoConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.context.annotation.ImportCandidates
import org.springframework.boot.test.context.FilteredClassLoader
import org.springframework.test.util.ReflectionTestUtils

class ChawpiAgentWorkflowAutoConfigurationTest {
    private val all =
        AutoConfigurations.of(
            ChawpiWorkflowAutoConfiguration::class.java,
            ChawpiAgentAutoConfiguration::class.java,
            ChawpiAgentWorkflowAutoConfiguration::class.java
        )

    @Test
    fun `with workflow installed, the assistant asks it`() {
        ChawpiContextRunner.core().withConfiguration(all).run { context ->
            assertThat(context).hasNotFailed()
            assertThat(ReflectionTestUtils.getField(context.getBean(AgentTools::class.java), "transitions"))
                .isInstanceOf(WorkflowRecordTransitions::class.java)
        }
    }

    @Test
    fun `with workflow switched off, the assistant says there are none`() {
        ChawpiContextRunner.core().withConfiguration(all).withPropertyValues("chawpi.workflow.enabled=false").run { context ->
            assertThat(context).hasNotFailed()
            assertThat(ReflectionTestUtils.getField(context.getBean(AgentTools::class.java), "transitions"))
                .isInstanceOf(NoRecordTransitions::class.java)
        }
    }

    @Test
    fun `with the agent itself switched off, no RecordTransitions adapter is wired`() {
        ChawpiContextRunner.core().withConfiguration(all).withPropertyValues("chawpi.agent.enabled=false").run { context ->
            assertThat(context).hasNotFailed()
            assertThat(context).doesNotHaveBean("workflowRecordTransitions")
            assertThat(context).doesNotHaveBean(AgentTools::class.java)
        }
    }

    @Test
    fun `without workflow on the classpath, the agent boots alone`() {
        ChawpiContextRunner
            .core()
            .withClassLoader(FilteredClassLoader("chawpi.workflow"))
            .withConfiguration(AutoConfigurations.of(ChawpiAgentAutoConfiguration::class.java, ChawpiAgentWorkflowAutoConfiguration::class.java))
            .run { context ->
                assertThat(context).hasNotFailed()
                assertThat(context).doesNotHaveBean("workflowRecordTransitions")
                assertThat(ReflectionTestUtils.getField(context.getBean(AgentTools::class.java), "transitions"))
                    .isInstanceOf(NoRecordTransitions::class.java)
            }
    }

    @Test
    fun `the imports file registers both agent auto-configs`() {
        assertThat(ImportCandidates.load(AutoConfiguration::class.java, javaClass.classLoader).candidates)
            .contains("chawpi.agent.autoconfigure.ChawpiAgentAutoConfiguration", "chawpi.agent.autoconfigure.ChawpiAgentWorkflowAutoConfiguration")
    }
}
