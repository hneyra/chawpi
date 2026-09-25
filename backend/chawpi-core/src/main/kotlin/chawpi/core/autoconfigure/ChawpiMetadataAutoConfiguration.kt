package chawpi.core.autoconfigure

import chawpi.core.identity.AccessPolicy
import chawpi.core.identity.CurrentUser
import chawpi.core.metadata.CallerPermissionsController
import chawpi.core.metadata.CallerPermissionsService
import chawpi.core.metadata.CustomFieldRepository
import chawpi.core.metadata.CustomObjectRepository
import chawpi.core.metadata.FieldTypeHandler
import chawpi.core.metadata.FieldTypeRegistry
import chawpi.core.metadata.FieldUsage
import chawpi.core.metadata.MetadataMapper
import chawpi.core.metadata.MetadataService
import chawpi.core.metadata.ObjectController
import chawpi.core.metadata.ObjectMetadataController
import chawpi.core.metadata.ObjectRemovalListener
import chawpi.core.metadata.ObjectSchemaManager
import chawpi.core.metadata.RelationshipController
import chawpi.core.metadata.RelationshipMapper
import chawpi.core.metadata.RelationshipRepository
import chawpi.core.metadata.RelationshipService
import chawpi.core.metadata.SystemFieldController
import chawpi.core.platform.ChawpiSchemas
import chawpi.core.platform.SystemColumns
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.r2dbc.core.DatabaseClient
import tools.jackson.databind.json.JsonMapper

// custom objects, fields, relationships and the field-type registry modules extend
@AutoConfiguration(after = [ChawpiSecurityAutoConfiguration::class])
class ChawpiMetadataAutoConfiguration {
    // core's twelve types first, then every FieldTypeHandler bean in @Order
    @Bean
    @ConditionalOnMissingBean
    fun fieldTypeRegistry(handlers: ObjectProvider<FieldTypeHandler>): FieldTypeRegistry = FieldTypeRegistry(handlers.orderedStream().toList())

    @Bean
    @ConditionalOnMissingBean
    fun customObjectRepository(
        db: DatabaseClient,
        schemas: ChawpiSchemas
    ): CustomObjectRepository = CustomObjectRepository(db, schemas)

    // JsonMapper, not the wider ObjectMapper: Boot 4.1's JacksonAutoConfiguration exposes a
    // JsonMapper bean (jackson 3). asking for the narrower type binds to it unambiguously even if
    // an app also has some other ObjectMapper-typed bean lying around.
    @Bean
    @ConditionalOnMissingBean
    fun customFieldRepository(
        db: DatabaseClient,
        objectMapper: JsonMapper,
        schemas: ChawpiSchemas,
        types: FieldTypeRegistry
    ): CustomFieldRepository = CustomFieldRepository(db, objectMapper, schemas, types)

    @Bean
    @ConditionalOnMissingBean
    fun relationshipRepository(
        db: DatabaseClient,
        schemas: ChawpiSchemas
    ): RelationshipRepository = RelationshipRepository(db, schemas)

    @Bean
    @ConditionalOnMissingBean
    fun objectSchemaManager(
        db: DatabaseClient,
        schemas: ChawpiSchemas,
        types: FieldTypeRegistry
    ): ObjectSchemaManager = ObjectSchemaManager(db, schemas, types)

    @Bean
    @ConditionalOnMissingBean
    fun metadataService(
        objects: CustomObjectRepository,
        fields: CustomFieldRepository,
        relationships: RelationshipRepository,
        schema: ObjectSchemaManager,
        currentUser: CurrentUser,
        access: AccessPolicy,
        types: FieldTypeRegistry,
        systemColumns: SystemColumns,
        usages: ObjectProvider<FieldUsage>,
        removals: ObjectProvider<ObjectRemovalListener>
    ): MetadataService =
        MetadataService(
            objects,
            fields,
            relationships,
            schema,
            currentUser,
            access,
            types,
            systemColumns,
            usages.orderedStream().toList(),
            removals.orderedStream().toList()
        )

    @Bean
    @ConditionalOnMissingBean
    fun relationshipService(
        relationships: RelationshipRepository,
        objects: CustomObjectRepository,
        fields: CustomFieldRepository,
        metadata: MetadataService,
        schema: ObjectSchemaManager,
        currentUser: CurrentUser
    ): RelationshipService = RelationshipService(relationships, objects, fields, metadata, schema, currentUser)

    @Bean
    @ConditionalOnMissingBean
    fun metadataMapper(
        objects: CustomObjectRepository,
        types: FieldTypeRegistry
    ): MetadataMapper = MetadataMapper(objects, types)

    @Bean
    @ConditionalOnMissingBean
    fun relationshipMapper(
        objects: CustomObjectRepository,
        fields: CustomFieldRepository
    ): RelationshipMapper = RelationshipMapper(objects, fields)

    @Bean
    @ConditionalOnMissingBean
    fun callerPermissionsService(
        objects: CustomObjectRepository,
        currentUser: CurrentUser
    ): CallerPermissionsService = CallerPermissionsService(objects, currentUser)

    @Bean
    @ConditionalOnMissingBean
    fun objectController(
        metadata: MetadataService,
        mapper: MetadataMapper,
        currentUser: CurrentUser
    ): ObjectController = ObjectController(metadata, mapper, currentUser)

    @Bean
    @ConditionalOnMissingBean
    fun objectMetadataController(
        metadata: MetadataService,
        mapper: MetadataMapper,
        currentUser: CurrentUser
    ): ObjectMetadataController = ObjectMetadataController(metadata, mapper, currentUser)

    @Bean
    @ConditionalOnMissingBean
    fun systemFieldController(systemColumns: SystemColumns): SystemFieldController = SystemFieldController(systemColumns)

    @Bean
    @ConditionalOnMissingBean
    fun relationshipController(
        relationships: RelationshipService,
        mapper: RelationshipMapper,
        currentUser: CurrentUser
    ): RelationshipController = RelationshipController(relationships, mapper, currentUser)

    @Bean
    @ConditionalOnMissingBean
    fun callerPermissionsController(permissions: CallerPermissionsService): CallerPermissionsController = CallerPermissionsController(permissions)
}
