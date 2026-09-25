package chawpi.forms.autoconfigure

import chawpi.core.autoconfigure.ChawpiDataAutoConfiguration
import chawpi.core.identity.CurrentUser
import chawpi.core.metadata.MetadataService
import chawpi.core.platform.ChawpiSchemas
import chawpi.core.platform.ModuleMigration
import chawpi.forms.FormController
import chawpi.forms.FormMetadataController
import chawpi.forms.FormRepository
import chawpi.forms.FormService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.r2dbc.core.DatabaseClient
import tools.jackson.databind.json.JsonMapper

// named forms. no scanning: every bean here, each one replaceable by the app.
@AutoConfiguration(after = [ChawpiDataAutoConfiguration::class])
@ConditionalOnProperty(prefix = "chawpi.forms", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ChawpiFormsProperties::class)
class ChawpiFormsAutoConfiguration {
    // never @ConditionalOnMissingBean: core's own ModuleMigration would always make it back off
    @Bean
    fun chawpiFormsMigration(): ModuleMigration = ModuleMigration("forms", "classpath:db/chawpi/forms", ModuleMigration.MODULE_ORDER)

    @Bean
    @ConditionalOnMissingBean
    fun formRepository(
        db: DatabaseClient,
        objectMapper: JsonMapper,
        schemas: ChawpiSchemas
    ): FormRepository = FormRepository(db, objectMapper, schemas)

    @Bean
    @ConditionalOnMissingBean
    fun formService(
        forms: FormRepository,
        metadata: MetadataService,
        currentUser: CurrentUser
    ): FormService = FormService(forms, metadata, currentUser)

    @Bean
    @ConditionalOnMissingBean
    fun formController(forms: FormService): FormController = FormController(forms)

    @Bean
    @ConditionalOnMissingBean
    fun formMetadataController(forms: FormService): FormMetadataController = FormMetadataController(forms)
}
