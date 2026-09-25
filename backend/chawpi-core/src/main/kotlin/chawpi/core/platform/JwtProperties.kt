package chawpi.core.platform

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

// no default secret on purpose: a library must not ship one that works
@ConfigurationProperties(prefix = "chawpi.security.jwt")
data class JwtProperties(
    val secret: String = "",
    val issuer: String = "chawpi",
    val ttl: Duration = Duration.ofHours(8)
)
