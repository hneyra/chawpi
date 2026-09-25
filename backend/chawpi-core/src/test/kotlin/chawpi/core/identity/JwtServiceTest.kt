package chawpi.core.identity

import chawpi.core.platform.JwtProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import java.time.Duration
import java.util.UUID
import javax.crypto.spec.SecretKeySpec

class JwtServiceTest {
    private val properties = JwtProperties(secret = "0123456789abcdef0123456789abcdef", issuer = "chawpi", ttl = Duration.ofHours(1))
    private val key = SecretKeySpec(properties.secret.toByteArray(Charsets.UTF_8), "HmacSHA256")

    @Test
    fun `issues a token the resource server accepts, carrying tenant, email and roles`() {
        val user =
            User(
                id = UUID.randomUUID(),
                organizationId = UUID.randomUUID(),
                email = "ana@chawpi.local",
                passwordHash = "unused",
                displayName = "Ana"
            )

        val issued = JwtService(properties, ChawpiJwtKey(key)).issue(user, listOf("ADMIN"))

        val jwt =
            NimbusReactiveJwtDecoder
                .withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build()
                .decode(issued.token)
                .block()!!
        assertThat(jwt.subject).isEqualTo(user.id.toString())
        assertThat(jwt.getClaimAsString(JwtService.CLAIM_ORGANIZATION)).isEqualTo(user.organizationId.toString())
        assertThat(jwt.getClaimAsString(JwtService.CLAIM_EMAIL)).isEqualTo("ana@chawpi.local")
        assertThat(jwt.getClaimAsStringList(JwtService.CLAIM_ROLES)).containsExactly("ADMIN")
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("chawpi")
    }
}
