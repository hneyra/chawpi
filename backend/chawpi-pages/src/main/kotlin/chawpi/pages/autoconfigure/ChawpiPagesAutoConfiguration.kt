package chawpi.pages.autoconfigure

import chawpi.core.autoconfigure.ChawpiDataAutoConfiguration
import chawpi.core.data.WorkflowStates
import chawpi.core.identity.CurrentUser
import chawpi.core.metadata.MetadataService
import chawpi.core.metadata.RelationshipService
import chawpi.core.platform.ChawpiSchemas
import chawpi.core.platform.ModuleMigration
import chawpi.forms.FormService
import chawpi.forms.autoconfigure.ChawpiFormsAutoConfiguration
import chawpi.pages.ObjectPageController
import chawpi.pages.PageComponentProvider
import chawpi.pages.PageComponentTypes
import chawpi.pages.PageController
import chawpi.pages.PageMetadataController
import chawpi.pages.PageRepository
import chawpi.pages.PageService
import chawpi.pages.PageTemplateController
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.r2dbc.core.DatabaseClient
import tools.jackson.databind.json.JsonMapper

// record pages. after forms, and only with its FormService: a FORM component names a stored form.
// module components (MAP, WORKFLOW) arrive as PageComponentProvider beans, found through a provider.
@AutoConfiguration(after = [ChawpiDataAutoConfiguration::class, ChawpiFormsAutoConfiguration::class])
@ConditionalOnProperty(prefix = "chawpi.pages", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(FormService::class)
@EnableConfigurationProperties(ChawpiPagesProperties::class)
class ChawpiPagesAutoConfiguration {
    // never @ConditionalOnMissingBean: core's own ModuleMigration would always make it back off
    @Bean
    fun chawpiPagesMigration(): ModuleMigration = ModuleMigration("pages", "classpath:db/chawpi/pages", ModuleMigration.MODULE_ORDER)

    @Bean
    @ConditionalOnMissingBean
    fun pageComponentTypes(providers: ObjectProvider<PageComponentProvider>): PageComponentTypes = PageComponentTypes(providers.orderedStream().toList())

    @Bean
    @ConditionalOnMissingBean
    fun pageRepository(
        db: DatabaseClient,
        objectMapper: JsonMapper,
        schemas: ChawpiSchemas
    ): PageRepository = PageRepository(db, objectMapper, schemas)

    @Bean
    @ConditionalOnMissingBean
    fun pageService(
        workflows: WorkflowStates,
        pages: PageRepository,
        metadata: MetadataService,
        relationships: RelationshipService,
        forms: FormService,
        currentUser: CurrentUser,
        componentTypes: PageComponentTypes
    ): PageService = PageService(workflows, pages, metadata, relationships, forms, currentUser, componentTypes)

    @Bean
    @ConditionalOnMissingBean
    fun pageController(pages: PageService): PageController = PageController(pages)

    @Bean
    @ConditionalOnMissingBean
    fun objectPageController(pages: PageService): ObjectPageController = ObjectPageController(pages)

    @Bean
    @ConditionalOnMissingBean
    fun pageTemplateController(): PageTemplateController = PageTemplateController()

    @Bean
    @ConditionalOnMissingBean
    fun pageMetadataController(pages: PageService): PageMetadataController = PageMetadataController(pages)
}
