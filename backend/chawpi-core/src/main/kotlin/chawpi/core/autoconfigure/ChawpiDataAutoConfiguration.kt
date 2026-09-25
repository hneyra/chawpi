package chawpi.core.autoconfigure

import chawpi.core.audit.AuditController
import chawpi.core.audit.AuditQueryService
import chawpi.core.audit.AuditService
import chawpi.core.data.NoWorkflowStates
import chawpi.core.data.PhysicalTableRecordStore
import chawpi.core.data.RecordChangeListener
import chawpi.core.data.RecordController
import chawpi.core.data.RecordQueryContributor
import chawpi.core.data.RecordQueryParser
import chawpi.core.data.RecordService
import chawpi.core.data.RecordStore
import chawpi.core.data.RelatedRecordController
import chawpi.core.data.RelatedRecordService
import chawpi.core.data.WorkflowStates
import chawpi.core.identity.AccessPolicy
import chawpi.core.identity.CurrentUser
import chawpi.core.metadata.CustomFieldRepository
import chawpi.core.metadata.CustomObjectRepository
import chawpi.core.metadata.FieldTypeRegistry
import chawpi.core.metadata.MetadataService
import chawpi.core.metadata.RelationshipMapper
import chawpi.core.metadata.RelationshipRepository
import chawpi.core.metadata.RelationshipService
import chawpi.core.platform.ChawpiSchemas
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.r2dbc.core.DatabaseClient
import tools.jackson.databind.json.JsonMapper

// records, audit and related records. a module that gives records a state, or stores them another
// way, declares its bean in an auto-config that runs before this one.
@AutoConfiguration(after = [ChawpiMetadataAutoConfiguration::class])
class ChawpiDataAutoConfiguration {
    // JsonMapper, not the wider ObjectMapper: see ChawpiMetadataAutoConfiguration.customFieldRepository.
    @Bean
    @ConditionalOnMissingBean
    fun auditService(
        db: DatabaseClient,
        objectMapper: JsonMapper,
        schemas: ChawpiSchemas
    ): AuditService = AuditService(db, objectMapper, schemas)

    @Bean
    @ConditionalOnMissingBean
    fun auditQueryService(
        db: DatabaseClient,
        objectMapper: JsonMapper,
        currentUser: CurrentUser,
        metadata: MetadataService,
        access: AccessPolicy,
        schemas: ChawpiSchemas
    ): AuditQueryService = AuditQueryService(db, objectMapper, currentUser, metadata, access, schemas)

    @Bean
    @ConditionalOnMissingBean
    fun auditController(audit: AuditQueryService): AuditController = AuditController(audit)

    // null object: no module installed, no object has a state (R9)
    @Bean
    @ConditionalOnMissingBean
    fun workflowStates(): WorkflowStates = NoWorkflowStates()

    @Bean
    @ConditionalOnMissingBean
    fun recordStore(
        db: DatabaseClient,
        schemas: ChawpiSchemas,
        types: FieldTypeRegistry
    ): RecordStore = PhysicalTableRecordStore(db, schemas, types)

    @Bean
    @ConditionalOnMissingBean
    fun recordQueryParser(contributors: ObjectProvider<RecordQueryContributor>): RecordQueryParser = RecordQueryParser(contributors.orderedStream().toList())

    @Bean
    @ConditionalOnMissingBean
    fun recordService(
        metadata: MetadataService,
        store: RecordStore,
        audit: AuditService,
        currentUser: CurrentUser,
        access: AccessPolicy,
        workflows: WorkflowStates,
        types: FieldTypeRegistry,
        changes: ObjectProvider<RecordChangeListener>
    ): RecordService = RecordService(metadata, store, audit, currentUser, access, workflows, types, changes.orderedStream().toList())

    @Bean
    @ConditionalOnMissingBean
    fun relatedRecordService(
        relationships: RelationshipRepository,
        relationshipService: RelationshipService,
        objects: CustomObjectRepository,
        fields: CustomFieldRepository,
        metadata: MetadataService,
        store: RecordStore,
        currentUser: CurrentUser,
        access: AccessPolicy,
        db: DatabaseClient,
        schemas: ChawpiSchemas,
        audit: AuditService
    ): RelatedRecordService =
        RelatedRecordService(relationships, relationshipService, objects, fields, metadata, store, currentUser, access, db, schemas, audit)

    @Bean
    @ConditionalOnMissingBean
    fun recordController(
        records: RecordService,
        queries: RecordQueryParser
    ): RecordController = RecordController(records, queries)

    @Bean
    @ConditionalOnMissingBean
    fun relatedRecordController(
        relationships: RelationshipService,
        related: RelatedRecordService,
        mapper: RelationshipMapper,
        queries: RecordQueryParser
    ): RelatedRecordController = RelatedRecordController(relationships, related, mapper, queries)
}
