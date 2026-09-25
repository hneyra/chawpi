package chawpi.workflow.autoconfigure

import chawpi.core.audit.AuditService
import chawpi.core.autoconfigure.ChawpiDataAutoConfiguration
import chawpi.core.data.RecordChangeListener
import chawpi.core.data.RecordStore
import chawpi.core.identity.AccessPolicy
import chawpi.core.identity.CurrentUser
import chawpi.core.identity.RoleDirectory
import chawpi.core.metadata.MetadataService
import chawpi.core.metadata.ObjectSchemaManager
import chawpi.core.platform.ChawpiSchemas
import chawpi.core.platform.ModuleMigration
import chawpi.workflow.WorkflowController
import chawpi.workflow.WorkflowRepository
import chawpi.workflow.WorkflowService
import chawpi.workflow.WorkflowStatesAdapter
import chawpi.workflow.WorkflowSystemColumns
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.r2dbc.core.DatabaseClient
import tools.jackson.databind.json.JsonMapper

// record states. before core's data config: our WorkflowStates must exist when core decides
// whether it still needs its NoWorkflowStates null object (P1 R9).
@AutoConfiguration(before = [ChawpiDataAutoConfiguration::class])
@ConditionalOnProperty(prefix = "chawpi.workflow", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ChawpiWorkflowProperties::class)
class ChawpiWorkflowAutoConfiguration {
    // never @ConditionalOnMissingBean: core's own ModuleMigration would always make it back off
    @Bean
    fun chawpiWorkflowMigration(): ModuleMigration = ModuleMigration("workflow", "classpath:db/chawpi/workflow", ModuleMigration.MODULE_ORDER)

    @Bean
    @ConditionalOnMissingBean
    fun workflowSystemColumns(): WorkflowSystemColumns = WorkflowSystemColumns()

    @Bean
    @ConditionalOnMissingBean
    fun workflowRepository(
        db: DatabaseClient,
        objectMapper: JsonMapper,
        schemas: ChawpiSchemas
    ): WorkflowRepository = WorkflowRepository(db, objectMapper, schemas)

    // named apart from core's "workflowStates" bean: same name would be a bean override, not a replacement
    @Bean
    @ConditionalOnMissingBean
    fun workflowStatesAdapter(workflows: WorkflowRepository): WorkflowStatesAdapter = WorkflowStatesAdapter(workflows)

    @Bean
    @ConditionalOnMissingBean
    fun workflowService(
        roles: RoleDirectory,
        workflows: WorkflowRepository,
        metadata: MetadataService,
        schema: ObjectSchemaManager,
        store: RecordStore,
        audit: AuditService,
        currentUser: CurrentUser,
        access: AccessPolicy,
        changes: ObjectProvider<RecordChangeListener>
    ): WorkflowService = WorkflowService(roles, workflows, metadata, schema, store, audit, currentUser, access, changes.orderedStream().toList())

    @Bean
    @ConditionalOnMissingBean
    fun workflowController(workflows: WorkflowService): WorkflowController = WorkflowController(workflows)
}
