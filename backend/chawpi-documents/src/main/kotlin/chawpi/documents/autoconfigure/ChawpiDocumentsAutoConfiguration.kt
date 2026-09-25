package chawpi.documents.autoconfigure

import chawpi.core.audit.AuditService
import chawpi.core.autoconfigure.ChawpiDataAutoConfiguration
import chawpi.core.data.RecordStore
import chawpi.core.data.RelatedRecordService
import chawpi.core.identity.CurrentUser
import chawpi.core.metadata.MetadataService
import chawpi.core.metadata.RelationshipService
import chawpi.core.platform.ChawpiSchemas
import chawpi.core.platform.ModuleMigration
import chawpi.documents.DocumentController
import chawpi.documents.DocumentCounterRepository
import chawpi.documents.DocumentRepository
import chawpi.documents.DocumentService
import chawpi.documents.DocumentTypeController
import chawpi.documents.DocumentTypeRepository
import chawpi.documents.DocumentTypeService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.r2dbc.core.DatabaseClient
import tools.jackson.databind.json.JsonMapper

// document types and issued documents. the automation port adapter is a separate auto-config
// (ChawpiDocumentsAutomationAutoConfiguration) that only exists when chawpi-automation does.
@AutoConfiguration(after = [ChawpiDataAutoConfiguration::class])
@ConditionalOnProperty(prefix = "chawpi.documents", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ChawpiDocumentsProperties::class)
class ChawpiDocumentsAutoConfiguration {
    // never @ConditionalOnMissingBean: core's own ModuleMigration would always make it back off
    @Bean
    fun chawpiDocumentsMigration(): ModuleMigration = ModuleMigration("documents", "classpath:db/chawpi/documents", ModuleMigration.MODULE_ORDER)

    @Bean
    @ConditionalOnMissingBean
    fun documentRepository(
        db: DatabaseClient,
        objectMapper: JsonMapper,
        schemas: ChawpiSchemas
    ): DocumentRepository = DocumentRepository(db, objectMapper, schemas)

    @Bean
    @ConditionalOnMissingBean
    fun documentCounterRepository(
        db: DatabaseClient,
        schemas: ChawpiSchemas
    ): DocumentCounterRepository = DocumentCounterRepository(db, schemas)

    @Bean
    @ConditionalOnMissingBean
    fun documentTypeRepository(
        db: DatabaseClient,
        objectMapper: JsonMapper,
        schemas: ChawpiSchemas
    ): DocumentTypeRepository = DocumentTypeRepository(db, objectMapper, schemas)

    @Bean
    @ConditionalOnMissingBean
    fun documentService(
        documents: DocumentRepository,
        counters: DocumentCounterRepository,
        types: DocumentTypeRepository,
        metadata: MetadataService,
        related: RelatedRecordService,
        store: RecordStore,
        currentUser: CurrentUser,
        audit: AuditService
    ): DocumentService = DocumentService(documents, counters, types, metadata, related, store, currentUser, audit)

    @Bean
    @ConditionalOnMissingBean
    fun documentTypeService(
        types: DocumentTypeRepository,
        documents: DocumentRepository,
        metadata: MetadataService,
        relationships: RelationshipService,
        currentUser: CurrentUser
    ): DocumentTypeService = DocumentTypeService(types, documents, metadata, relationships, currentUser)

    @Bean
    @ConditionalOnMissingBean
    fun documentController(documents: DocumentService): DocumentController = DocumentController(documents)

    @Bean
    @ConditionalOnMissingBean
    fun documentTypeController(types: DocumentTypeService): DocumentTypeController = DocumentTypeController(types)
}
