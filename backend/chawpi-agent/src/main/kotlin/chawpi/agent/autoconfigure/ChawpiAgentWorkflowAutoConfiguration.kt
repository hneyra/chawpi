package chawpi.agent.autoconfigure

import chawpi.agent.RecordTransitions
import chawpi.agent.WorkflowRecordTransitions
import chawpi.workflow.WorkflowService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean

// workflow behind the assistant's transitions port, only when chawpi-workflow is on the classpath
// and switched on. names as strings: this config is skipped before anything loads a workflow class.
// the agent looks the port up lazily, so no order against the agent's own auto-config is needed.
// gated on chawpi.agent, not chawpi.workflow: this is the agent's own wiring, off with the agent.
@AutoConfiguration(afterName = ["chawpi.workflow.autoconfigure.ChawpiWorkflowAutoConfiguration"])
@ConditionalOnClass(name = ["chawpi.workflow.WorkflowService"])
@ConditionalOnBean(type = ["chawpi.workflow.WorkflowService"])
@ConditionalOnProperty(prefix = "chawpi.agent", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class ChawpiAgentWorkflowAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(RecordTransitions::class)
    fun workflowRecordTransitions(workflows: WorkflowService): WorkflowRecordTransitions = WorkflowRecordTransitions(workflows)
}
