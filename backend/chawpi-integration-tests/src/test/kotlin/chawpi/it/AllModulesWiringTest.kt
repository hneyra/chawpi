package chawpi.it

import chawpi.agent.AgentTools
import chawpi.agent.WorkflowRecordTransitions
import chawpi.agent.autoconfigure.ChawpiAgentAutoConfiguration
import chawpi.agent.autoconfigure.ChawpiAgentWorkflowAutoConfiguration
import chawpi.automation.AutomationService
import chawpi.automation.autoconfigure.ChawpiAutomationAutoConfiguration
import chawpi.core.data.WorkflowStates
import chawpi.core.metadata.FieldTypeRegistry
import chawpi.core.platform.ModuleMigration
import chawpi.core.platform.SystemColumns
import chawpi.documents.DocumentIssuerAdapter
import chawpi.documents.autoconfigure.ChawpiDocumentsAutoConfiguration
import chawpi.documents.autoconfigure.ChawpiDocumentsAutomationAutoConfiguration
import chawpi.forms.autoconfigure.ChawpiFormsAutoConfiguration
import chawpi.gis.GEOMETRY
import chawpi.gis.MapPageComponent
import chawpi.gis.autoconfigure.ChawpiGisAutoConfiguration
import chawpi.gis.autoconfigure.ChawpiGisPagesAutoConfiguration
import chawpi.pages.ComponentType
import chawpi.pages.PageComponentTypes
import chawpi.pages.autoconfigure.ChawpiPagesAutoConfiguration
import chawpi.test.ChawpiContextRunner
import chawpi.views.autoconfigure.ChawpiViewsAutoConfiguration
import chawpi.workflow.WorkflowPageComponent
import chawpi.workflow.WorkflowStatesAdapter
import chawpi.workflow.autoconfigure.ChawpiWorkflowAutoConfiguration
import chawpi.workflow.autoconfigure.ChawpiWorkflowPagesAutoConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.reactive.config.EnableWebFlux
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping

class AllModulesWiringTest {
    // a handler mapping, so every controller's routes are registered and an ambiguous one fails the
    // context. no @Configuration on purpose: P3's scanned test apps in this package must not pick it up.
    @EnableWebFlux
    class WebFlux

    private val runner =
        ChawpiContextRunner
            .core()
            .withUserConfiguration(WebFlux::class.java)
            .withConfiguration(
                AutoConfigurations.of(
                    ChawpiViewsAutoConfiguration::class.java,
                    ChawpiFormsAutoConfiguration::class.java,
                    ChawpiPagesAutoConfiguration::class.java,
                    ChawpiWorkflowAutoConfiguration::class.java,
                    ChawpiWorkflowPagesAutoConfiguration::class.java,
                    ChawpiAutomationAutoConfiguration::class.java,
                    ChawpiDocumentsAutoConfiguration::class.java,
                    ChawpiDocumentsAutomationAutoConfiguration::class.java,
                    ChawpiGisAutoConfiguration::class.java,
                    ChawpiGisPagesAutoConfiguration::class.java,
                    ChawpiAgentAutoConfiguration::class.java,
                    ChawpiAgentWorkflowAutoConfiguration::class.java
                )
            ).withPropertyValues("chawpi.automation.poll-interval=0s")

    @Test
    fun `every module wires with every optional link made`() {
        runner.run { context ->
            assertThat(context).hasNotFailed()
            assertThat(context.getBeansOfType(ModuleMigration::class.java).values.map { it.name })
                .containsExactlyInAnyOrder("core", "views", "forms", "pages", "workflow", "automation", "documents", "gis")
            assertThat(context.getBean(FieldTypeRegistry::class.java).types.last()).isEqualTo(GEOMETRY)
            assertThat(context.getBean(SystemColumns::class.java).names).contains("workflow_state")
            assertThat(context.getBean(WorkflowStates::class.java)).isInstanceOf(WorkflowStatesAdapter::class.java)
            val components = context.getBean(PageComponentTypes::class.java)
            assertThat(components.provider(ComponentType("MAP"))).isInstanceOf(MapPageComponent::class.java)
            assertThat(components.provider(ComponentType("WORKFLOW"))).isInstanceOf(WorkflowPageComponent::class.java)
            assertThat(ReflectionTestUtils.getField(context.getBean(AutomationService::class.java), "documents"))
                .isInstanceOf(DocumentIssuerAdapter::class.java)
            assertThat(ReflectionTestUtils.getField(context.getBean(AgentTools::class.java), "transitions"))
                .isInstanceOf(WorkflowRecordTransitions::class.java)
        }
    }

    // the original's module routes, verb by verb, plus the three metadata routes that left core (P1 R16)
    @Test
    fun `the module routes are the original's, with no collision`() {
        runner.run { context ->
            assertThat(context).hasNotFailed()
            val routes =
                context.getBean(RequestMappingHandlerMapping::class.java).handlerMethods.keys.flatMap { info ->
                    info.methodsCondition.methods.flatMap { verb -> info.patternsCondition.patterns.map { "${verb.name} ${it.patternString}" } }
                }
            assertThat(routes).doesNotHaveDuplicates().containsAll(LEGACY_MODULE_ROUTES)
        }
    }

    companion object {
        val LEGACY_MODULE_ROUTES =
            listOf(
                "GET /api/metadata/objects/{object}/views",
                "GET /api/metadata/objects/{object}/forms",
                "GET /api/metadata/objects/{object}/pages",
                "DELETE /api/gis/layers/{object}",
                "DELETE /api/gis/layers/{object}/{geometry}",
                "DELETE /api/objects/{object}/automations/{name}",
                "DELETE /api/objects/{object}/document-types/{name}",
                "DELETE /api/objects/{object}/forms/{name}",
                "DELETE /api/objects/{object}/views/{name}",
                "DELETE /api/objects/{object}/workflow",
                "DELETE /api/pages/{name}",
                "GET /api/agent/status",
                "GET /api/automation-runs",
                "GET /api/documents/{id}",
                "GET /api/gis/layers",
                "GET /api/gis/objects/{object}/features",
                "GET /api/gis/objects/{object}/features/{id}",
                "GET /api/gis/services",
                "GET /api/metadata/page-templates",
                "GET /api/objects/{object}/automations",
                "GET /api/objects/{object}/automations/{name}",
                "GET /api/objects/{object}/automations/{name}/runs",
                "GET /api/objects/{object}/document-types",
                "GET /api/objects/{object}/document-types/{name}",
                "GET /api/objects/{object}/forms",
                "GET /api/objects/{object}/forms/{name}",
                "GET /api/objects/{object}/pages/{kind}",
                "GET /api/objects/{object}/records/{id}/documents",
                "GET /api/objects/{object}/records/{id}/transitions",
                "GET /api/objects/{object}/views",
                "GET /api/objects/{object}/views/{name}",
                "GET /api/objects/{object}/workflow",
                "GET /api/pages",
                "GET /api/pages/{name}",
                "POST /api/agent/ask",
                "POST /api/gis/layers/{object}",
                "POST /api/gis/layers/{object}/{geometry}",
                "POST /api/objects/{object}/automations",
                "POST /api/objects/{object}/document-types",
                "POST /api/objects/{object}/forms",
                "POST /api/objects/{object}/records/{id}/documents/{type}",
                "POST /api/objects/{object}/records/{id}/transitions/{name}",
                "POST /api/objects/{object}/views",
                "POST /api/pages",
                "PUT /api/objects/{object}/automations/{name}",
                "PUT /api/objects/{object}/document-types/{name}",
                "PUT /api/objects/{object}/forms/{name}",
                "PUT /api/objects/{object}/views/{name}",
                "PUT /api/objects/{object}/workflow",
                "PUT /api/pages/{name}"
            )
    }
}
