package chawpi.core.platform

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "chawpi.web")
data class ChawpiWebProperties(
    // problem+json "type" is this plus "/<status>"
    val problemBaseUri: String = "https://chawpi.dev/problems",
    // browser origins the api answers. dev servers by default.
    val corsAllowedOriginPatterns: List<String> = listOf("http://localhost:*")
)
