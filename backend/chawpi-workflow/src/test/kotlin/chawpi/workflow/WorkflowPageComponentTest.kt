package chawpi.workflow

import chawpi.core.data.ObjectWorkflowState
import chawpi.core.data.WorkflowStates
import chawpi.core.metadata.CustomObject
import chawpi.core.metadata.ObjectDefinition
import chawpi.pages.ComponentType
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class WorkflowPageComponentTest {
    private val definition =
        ObjectDefinition(
            CustomObject(UUID.randomUUID(), UUID.randomUUID(), "tramite", "Trámite", "Trámites", null, true, "tramite__00000000", null, null),
            emptyList()
        )

    private fun states(attached: Boolean) =
        object : WorkflowStates {
            override suspend fun stateOf(
                organizationId: UUID,
                objectId: UUID
            ) = if (attached) ObjectWorkflowState(true, "draft") else ObjectWorkflowState.NONE

            override suspend fun transitionNames(
                organizationId: UUID,
                objectId: UUID
            ) = emptySet<String>()
        }

    // acting on the record's state is something you do while looking at it, not at its trail
    @Test
    fun `an attached workflow sits in the details tab, next to the form`() =
        runTest {
            val generated = WorkflowPageComponent(states(attached = true)).generated(definition)!!
            assertThat(generated.tab).isNull()
            assertThat(generated.component.type).isEqualTo(ComponentType("WORKFLOW"))
        }

    @Test
    fun `no workflow, no component`() =
        runTest {
            assertThat(WorkflowPageComponent(states(attached = false)).generated(definition)).isNull()
        }
}
