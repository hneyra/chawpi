package chawpi.automation.autoconfigure

import chawpi.automation.AutomationController
import chawpi.automation.AutomationDispatcher
import chawpi.automation.AutomationDrain
import chawpi.automation.AutomationFieldUsage
import chawpi.automation.AutomationProperties
import chawpi.automation.AutomationRepository
import chawpi.automation.AutomationRunRepository
import chawpi.automation.AutomationRunner
import chawpi.automation.AutomationService
import chawpi.automation.DocumentIssuer
import chawpi.automation.NoDocumentIssuer
import chawpi.automation.WebhookSender
import chawpi.core.audit.AuditService
import chawpi.core.autoconfigure.ChawpiDataAutoConfiguration
import chawpi.core.data.RecordStore
import chawpi.core.data.WorkflowStates
import chawpi.core.identity.CurrentUser
import chawpi.core.metadata.MetadataService
import chawpi.core.platform.ChawpiSchemas
import chawpi.core.platform.ModuleMigration
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.r2dbc.core.DatabaseClient
import tools.jackson.databind.json.JsonMapper

// automations. the document port is looked up, not required: with no documents module the null
// issuer answers, and no auto-config order has to be right for that (M2).
@AutoConfiguration(after = [ChawpiDataAutoConfiguration::class])
@ConditionalOnProperty(prefix = "chawpi.automation", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AutomationProperties::class)
class ChawpiAutomationAutoConfiguration {
    // never @ConditionalOnMissingBean: core's own ModuleMigration would always make it back off
    @Bean
    fun chawpiAutomationMigration(): ModuleMigration = ModuleMigration("automation", "classpath:db/chawpi/automation", ModuleMigration.MODULE_ORDER)

    @Bean
    @ConditionalOnMissingBean
    fun automationRepository(
        db: DatabaseClient,
        objectMapper: JsonMapper,
        schemas: ChawpiSchemas
    ): AutomationRepository = AutomationRepository(db, objectMapper, schemas)

    @Bean
    @ConditionalOnMissingBean
    fun automationRunRepository(
        db: DatabaseClient,
        objectMapper: JsonMapper,
        schemas: ChawpiSchemas
    ): AutomationRunRepository = AutomationRunRepository(db, objectMapper, schemas)

    @Bean
    @ConditionalOnMissingBean
    fun automationDispatcher(
        automations: AutomationRepository,
        runs: AutomationRunRepository,
        properties: AutomationProperties
    ): AutomationDispatcher = AutomationDispatcher(automations, runs, properties)

    @Bean
    @ConditionalOnMissingBean
    fun automationFieldUsage(automations: AutomationRepository): AutomationFieldUsage = AutomationFieldUsage(automations)

    @Bean
    @ConditionalOnMissingBean
    fun webhookSender(properties: AutomationProperties): WebhookSender = WebhookSender(properties)

    @Bean
    @ConditionalOnMissingBean
    fun automationRunner(
        automations: AutomationRepository,
        runs: AutomationRunRepository,
        metadata: MetadataService,
        store: RecordStore,
        workflows: WorkflowStates,
        audit: AuditService,
        dispatcher: AutomationDispatcher,
        webhooks: WebhookSender,
        documents: ObjectProvider<DocumentIssuer>
    ): AutomationRunner =
        AutomationRunner(automations, runs, metadata, store, workflows, audit, dispatcher, webhooks, documents.getIfAvailable { NoDocumentIssuer() })

    @Bean
    @ConditionalOnMissingBean
    fun automationService(
        automations: AutomationRepository,
        runs: AutomationRunRepository,
        metadata: MetadataService,
        webhooks: WebhookSender,
        documents: ObjectProvider<DocumentIssuer>,
        currentUser: CurrentUser
    ): AutomationService = AutomationService(automations, runs, metadata, webhooks, documents.getIfAvailable { NoDocumentIssuer() }, currentUser)

    // polls from SmartLifecycle.start(), after every singleton (migrations included): no @DependsOn (M9)
    @Bean
    @ConditionalOnMissingBean
    fun automationDrain(
        runner: AutomationRunner,
        properties: AutomationProperties
    ): AutomationDrain = AutomationDrain(runner, properties)

    @Bean
    @ConditionalOnMissingBean
    fun automationController(automations: AutomationService): AutomationController = AutomationController(automations)
}
