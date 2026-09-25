package chawpi.workflow

import chawpi.core.data.WorkflowStates
import chawpi.core.metadata.ObjectDefinition
import chawpi.pages.ComponentType
import chawpi.pages.GeneratedComponent
import chawpi.pages.PageComponent
import chawpi.pages.PageComponentProvider
import org.springframework.core.annotation.Order

// the WORKFLOW page component: the record's state and the transitions open to the caller. it reads
// what the record already has, so there is nothing to configure and nothing to check.
// ordered after gis's MapPageComponent (100), so the generated tab order matches the original app: MAP then
// WORKFLOW.
@Order(200)
class WorkflowPageComponent(
    private val workflows: WorkflowStates
) : PageComponentProvider {
    override val type = WORKFLOW

    // acting on the record's state is something you do while looking at it, not at its trail:
    // it joins the details tab
    override suspend fun generated(definition: ObjectDefinition): GeneratedComponent? =
        if (workflows.stateOf(definition.obj.organizationId, definition.obj.id).attached) {
            GeneratedComponent(PageComponent(type = WORKFLOW))
        } else {
            null
        }

    companion object {
        val WORKFLOW = ComponentType("WORKFLOW")
    }
}
