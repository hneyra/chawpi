package chawpi.core.autoconfigure

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringApplication
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment

class ChawpiEnvironmentPostProcessorTest {
    private fun environment(app: Map<String, Any>): StandardEnvironment =
        StandardEnvironment().apply {
            propertySources.addFirst(MapPropertySource("app", app))
            ChawpiEnvironmentPostProcessor().postProcessEnvironment(this, SpringApplication())
        }

    @Test
    fun `r2dbc follows chawpi database settings`() {
        val env = environment(mapOf("chawpi.database.host" to "db.local", "chawpi.database.port" to "6543", "chawpi.database.name" to "acme"))

        assertThat(env.getProperty("spring.r2dbc.url")).isEqualTo("r2dbc:postgresql://db.local:6543/acme")
        assertThat(env.getProperty("spring.webflux.problemdetails.enabled")).isEqualTo("true")
    }

    @Test
    fun `whatever the app sets wins over the defaults`() {
        val env = environment(mapOf("spring.r2dbc.url" to "r2dbc:postgresql://elsewhere/x"))

        assertThat(env.getProperty("spring.r2dbc.url")).isEqualTo("r2dbc:postgresql://elsewhere/x")
    }

    @Test
    fun `no jwt secret unless the app gives one`() {
        assertThat(environment(emptyMap()).getProperty("chawpi.security.jwt.secret")).isEmpty()
    }
}
