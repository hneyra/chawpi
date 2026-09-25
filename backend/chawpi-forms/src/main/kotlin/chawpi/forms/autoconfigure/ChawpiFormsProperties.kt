package chawpi.forms.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("chawpi.forms")
data class ChawpiFormsProperties(
    // false: no forms beans, routes or migration (and chawpi-pages backs off with it)
    val enabled: Boolean = true
)
