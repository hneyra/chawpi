package chawpi.views.autoconfigure

import chawpi.core.autoconfigure.ChawpiDataAutoConfiguration
import chawpi.core.identity.CurrentUser
import chawpi.core.metadata.MetadataService
import chawpi.core.platform.ChawpiSchemas
import chawpi.core.platform.ModuleMigration
import chawpi.views.ViewController
import chawpi.views.ViewMetadataController
import chawpi.views.ViewRepository
import chawpi.views.ViewService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.r2dbc.core.DatabaseClient
import tools.jackson.databind.json.JsonMapper

// named list views. no scanning: every bean here, each one replaceable by the app.
@AutoConfiguration(after = [ChawpiDataAutoConfiguration::class])
@ConditionalOnProperty(prefix = "chawpi.views", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ChawpiViewsProperties::class)
class ChawpiViewsAutoConfiguration {
    // never @ConditionalOnMissingBean: core's own ModuleMigration would always make it back off
    @Bean
    fun chawpiViewsMigration(): ModuleMigration = ModuleMigration("views", "classpath:db/chawpi/views", ModuleMigration.MODULE_ORDER)

    @Bean
    @ConditionalOnMissingBean
    fun viewRepository(
        db: DatabaseClient,
        objectMapper: JsonMapper,
        schemas: ChawpiSchemas
    ): ViewRepository = ViewRepository(db, objectMapper, schemas)

    @Bean
    @ConditionalOnMissingBean
    fun viewService(
        views: ViewRepository,
        metadata: MetadataService,
        currentUser: CurrentUser
    ): ViewService = ViewService(views, metadata, currentUser)

    @Bean
    @ConditionalOnMissingBean
    fun viewController(views: ViewService): ViewController = ViewController(views)

    @Bean
    @ConditionalOnMissingBean
    fun viewMetadataController(views: ViewService): ViewMetadataController = ViewMetadataController(views)
}
