package chawpi.gis.autoconfigure

import chawpi.core.autoconfigure.ChawpiDataAutoConfiguration
import chawpi.core.data.RecordQueryParser
import chawpi.core.data.RecordService
import chawpi.core.identity.CurrentUser
import chawpi.core.metadata.FieldTypeRegistry
import chawpi.core.metadata.MetadataService
import chawpi.core.platform.ChawpiSchemas
import chawpi.core.platform.ModuleMigration
import chawpi.gis.BboxQuery
import chawpi.gis.FeatureController
import chawpi.gis.GeoServerClient
import chawpi.gis.GeoServerProperties
import chawpi.gis.GeometryFieldType
import chawpi.gis.LayerCleanup
import chawpi.gis.LayerController
import chawpi.gis.LayerService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import tools.jackson.databind.json.JsonMapper

// GIS as a plug-in: a field type, a query parameter, an object-removal listener and its own routes.
// core finds the first three through ObjectProviders, so no ordering against core is needed.
@AutoConfiguration(after = [ChawpiDataAutoConfiguration::class])
@ConditionalOnProperty(prefix = "chawpi.gis", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ChawpiGisProperties::class, GeoServerProperties::class)
class ChawpiGisAutoConfiguration {
    // never @ConditionalOnMissingBean: core's own ModuleMigration would always make it back off
    @Bean
    fun chawpiGisMigration(): ModuleMigration = ModuleMigration("gis", "classpath:db/chawpi/gis", ModuleMigration.MODULE_ORDER)

    @Bean
    @ConditionalOnMissingBean
    fun geometryFieldType(objectMapper: JsonMapper): GeometryFieldType = GeometryFieldType(objectMapper)

    @Bean
    @ConditionalOnMissingBean
    fun bboxQuery(): BboxQuery = BboxQuery()

    @Bean
    @ConditionalOnMissingBean
    fun geoServerClient(
        properties: GeoServerProperties,
        schemas: ChawpiSchemas
    ): GeoServerClient = GeoServerClient(properties, properties.datastore.schema ?: schemas.data)

    @Bean
    @ConditionalOnMissingBean
    fun layerCleanup(
        client: GeoServerClient,
        properties: GeoServerProperties
    ): LayerCleanup = LayerCleanup(client, properties)

    @Bean
    @ConditionalOnMissingBean
    fun layerService(
        metadata: MetadataService,
        client: GeoServerClient,
        properties: GeoServerProperties,
        currentUser: CurrentUser
    ): LayerService = LayerService(metadata, client, properties, currentUser)

    @Bean
    @ConditionalOnMissingBean
    fun layerController(layers: LayerService): LayerController = LayerController(layers)

    @Bean
    @ConditionalOnMissingBean
    fun featureController(
        records: RecordService,
        queries: RecordQueryParser,
        types: FieldTypeRegistry
    ): FeatureController = FeatureController(records, queries, types)
}
