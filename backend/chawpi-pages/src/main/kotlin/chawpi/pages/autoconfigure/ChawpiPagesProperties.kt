package chawpi.pages.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("chawpi.pages")
data class ChawpiPagesProperties(
    // false: no pages beans, routes or migration
    val enabled: Boolean = true
)
