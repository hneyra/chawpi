package chawpi.agent.autoconfigure

import chawpi.agent.AgentController
import chawpi.agent.AgentProperties
import chawpi.agent.AgentService
import chawpi.agent.AgentTools
import chawpi.agent.ChawpiAgent
import chawpi.agent.NoRecordTransitions
import chawpi.agent.RecordTransitions
import chawpi.core.audit.AuditQueryService
import chawpi.core.autoconfigure.ChawpiDataAutoConfiguration
import chawpi.core.data.RecordQueryParser
import chawpi.core.data.RecordService
import chawpi.core.data.RelatedRecordService
import chawpi.core.identity.CurrentUser
import chawpi.core.metadata.MetadataMapper
import chawpi.core.metadata.MetadataService
import chawpi.core.metadata.RelationshipService
import com.embabel.agent.core.AgentPlatform
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import tools.jackson.databind.json.JsonMapper

// the assistant. the tools only ever call services, so tenancy and permissions are the caller's.
// embabel finds ChawpiAgent by its @Agent annotation through a bean post-processor: a @Bean is enough.
@AutoConfiguration(after = [ChawpiDataAutoConfiguration::class])
@ConditionalOnProperty(prefix = "chawpi.agent", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AgentProperties::class)
class ChawpiAgentAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun agentTools(
        metadata: MetadataService,
        mapper: MetadataMapper,
        records: RecordService,
        relationships: RelationshipService,
        related: RelatedRecordService,
        transitions: ObjectProvider<RecordTransitions>,
        queries: RecordQueryParser,
        audit: AuditQueryService,
        currentUser: CurrentUser,
        json: JsonMapper
    ): AgentTools =
        AgentTools(
            metadata,
            mapper,
            records,
            relationships,
            related,
            transitions.getIfAvailable { NoRecordTransitions() },
            queries,
            audit,
            currentUser,
            json
        )

    @Bean
    @ConditionalOnMissingBean
    fun chawpiAgent(
        properties: AgentProperties,
        tools: AgentTools
    ): ChawpiAgent = ChawpiAgent(properties, tools)

    // the platform is absent whenever EmbabelGate kept embabel out (no key, or switched off)
    @Bean
    @ConditionalOnMissingBean
    fun agentService(
        properties: AgentProperties,
        currentUser: CurrentUser,
        platform: ObjectProvider<AgentPlatform>
    ): AgentService = AgentService(properties, currentUser, platform)

    @Bean
    @ConditionalOnMissingBean
    fun agentController(agent: AgentService): AgentController = AgentController(agent)
}
