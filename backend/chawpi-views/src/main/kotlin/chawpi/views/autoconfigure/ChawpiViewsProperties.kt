package chawpi.views.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("chawpi.views")
data class ChawpiViewsProperties(
    // false: no views beans, routes or migration
    val enabled: Boolean = true
)
