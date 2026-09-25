package chawpi.documents.autoconfigure

import chawpi.automation.DocumentIssuer
import chawpi.documents.DocumentIssuerAdapter
import chawpi.documents.DocumentService
import chawpi.documents.DocumentTypeRepository
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

// documents behind automation's port, only when chawpi-automation is on the classpath. the class
// is named as a string, so this config is skipped before anything tries to load it (M2).
// automation looks the port up lazily, so no order against its auto-config is needed.
@AutoConfiguration(after = [ChawpiDocumentsAutoConfiguration::class])
@ConditionalOnClass(name = ["chawpi.automation.DocumentIssuer"])
@ConditionalOnBean(DocumentService::class)
class ChawpiDocumentsAutomationAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(DocumentIssuer::class)
    fun documentIssuerAdapter(
        types: DocumentTypeRepository,
        documents: DocumentService
    ): DocumentIssuerAdapter = DocumentIssuerAdapter(types, documents)
}
