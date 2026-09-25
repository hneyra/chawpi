package chawpi.agent

import chawpi.core.audit.AuditService
import chawpi.core.data.RecordStore
import chawpi.core.identity.AccessPolicy
import chawpi.core.identity.CurrentUser
import chawpi.core.identity.RoleDirectory
import chawpi.core.metadata.MetadataService
import chawpi.core.metadata.ObjectSchemaManager
import chawpi.workflow.AvailableTransition
import chawpi.workflow.WorkflowRepository
import chawpi.workflow.WorkflowService
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

class WorkflowRecordTransitionsTest {
    private val available =
        listOf(
            AvailableTransition("finish", "Finish", "done", "Done", true),
            AvailableTransition("reject", "Reject", "rejected", "Rejected", false, "requires role 'reviewer'")
        )

    private val workflows =
        object : WorkflowService(
            mock(RoleDirectory::class.java),
            mock(WorkflowRepository::class.java),
            mock(MetadataService::class.java),
            mock(ObjectSchemaManager::class.java),
            mock(RecordStore::class.java),
            mock(AuditService::class.java),
            mock(CurrentUser::class.java),
            mock(AccessPolicy::class.java),
            emptyList()
        ) {
            override suspend fun transitionsOf(
                objectName: String,
                id: UUID
            ): List<AvailableTransition> = available
        }

    // the tool answer must read exactly as it did when the agent called WorkflowService itself
    @Test
    fun `transitions reach the assistant with the same json`() =
        runTest {
            val mapper = JsonMapper.builder().build()
            val answered = WorkflowRecordTransitions(workflows).transitionsOf("predio", UUID.randomUUID())
            assertThat(answered.count { it.allowed }).isEqualTo(1)
            assertThat(mapper.writeValueAsString(answered)).isEqualTo(mapper.writeValueAsString(available))
        }
}
