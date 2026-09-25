package chawpi.core.autoconfigure

import chawpi.core.common.GlobalExceptionHandler
import chawpi.core.common.HealthController
import chawpi.core.platform.ChawpiDatabaseProperties
import chawpi.core.platform.ChawpiMigrations
import chawpi.core.platform.ChawpiSchemas
import chawpi.core.platform.ChawpiWebProperties
import chawpi.core.platform.JwtProperties
import chawpi.core.platform.ModuleMigration
import chawpi.core.platform.SystemColumnContributor
import chawpi.core.platform.SystemColumns
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

// properties, schema names, system columns, migrations, errors, health. no scanning: every bean here.
@AutoConfiguration
@EnableConfigurationProperties(ChawpiDatabaseProperties::class, JwtProperties::class, ChawpiWebProperties::class)
class ChawpiPlatformAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun chawpiSchemas(database: ChawpiDatabaseProperties): ChawpiSchemas = ChawpiSchemas.of(database)

    @Bean
    @ConditionalOnMissingBean
    fun systemColumns(contributors: ObjectProvider<SystemColumnContributor>): SystemColumns = SystemColumns(contributors.orderedStream().toList())

    @Bean
    fun chawpiCoreMigration(): ModuleMigration = ModuleMigration.CORE

    @Bean
    @ConditionalOnProperty(prefix = "chawpi.seed", name = ["dev"], havingValue = "true")
    fun chawpiCoreSeedMigration(): ModuleMigration = ModuleMigration.CORE_SEED

    // runs at startup, before traffic: ChawpiMigrations.afterPropertiesSet() calls migrate(), so it
    // runs even if an app replaces this bean with its own instance. every module migration bean is
    // in the list. keep the bean name "chawpiMigrations" stable: modules @DependsOn it by name.
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "chawpi.database", name = ["migrate"], havingValue = "true", matchIfMissing = true)
    fun chawpiMigrations(
        database: ChawpiDatabaseProperties,
        schemas: ChawpiSchemas,
        migrations: ObjectProvider<ModuleMigration>
    ): ChawpiMigrations = ChawpiMigrations(database, schemas, migrations.orderedStream().toList())

    @Bean
    @ConditionalOnMissingBean
    fun globalExceptionHandler(web: ChawpiWebProperties): GlobalExceptionHandler = GlobalExceptionHandler(web.problemBaseUri)

    @Bean
    @ConditionalOnMissingBean
    fun healthController(
        @Value("\${spring.application.name:chawpi}") applicationName: String
    ): HealthController = HealthController(applicationName)
}
