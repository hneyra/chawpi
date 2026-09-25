package chawpi.workflow.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("chawpi.workflow")
data class ChawpiWorkflowProperties(
    // false: no workflow beans, routes or migration; records have no state
    val enabled: Boolean = true
)
