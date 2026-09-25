package chawpi.documents.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("chawpi.documents")
data class ChawpiDocumentsProperties(
    // false: no documents beans, routes or migration
    val enabled: Boolean = true
)
