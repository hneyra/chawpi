package chawpi.test

import chawpi.core.autoconfigure.ChawpiAdminAutoConfiguration
import chawpi.core.autoconfigure.ChawpiDataAutoConfiguration
import chawpi.core.autoconfigure.ChawpiMetadataAutoConfiguration
import chawpi.core.autoconfigure.ChawpiPlatformAutoConfiguration
import chawpi.core.autoconfigure.ChawpiSecurityAutoConfiguration
import org.mockito.Mockito.mock
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.security.autoconfigure.ReactiveUserDetailsServiceAutoConfiguration
import org.springframework.boot.security.autoconfigure.web.reactive.ReactiveWebSecurityAutoConfiguration
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.reactive.ReactiveOAuth2ResourceServerAutoConfiguration
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.reactive.ReactiveOAuth2ResourceServerWebSecurityAutoConfiguration
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner
import org.springframework.r2dbc.core.DatabaseClient
import tools.jackson.databind.json.JsonMapper

/**
 * Core's whole bean graph on a mocked database: enough to prove a module's auto-config wires,
 * backs off and joins core's SPIs, without docker. Migrations are off, so nothing connects.
 *
 * chawpi-test depends on chawpi-core as `api`, so this class's own POM carries the edge to it --
 * a consumer adding only `testImplementation("chawpi:chawpi-test")` gets a consistent core on the
 * test classpath for free.
 */
object ChawpiContextRunner {
    const val TEST_SECRET = "0123456789abcdef0123456789abcdef"

    fun core(): ReactiveWebApplicationContextRunner =
        ReactiveWebApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    ReactiveUserDetailsServiceAutoConfiguration::class.java,
                    ReactiveWebSecurityAutoConfiguration::class.java,
                    ReactiveOAuth2ResourceServerAutoConfiguration::class.java,
                    ReactiveOAuth2ResourceServerWebSecurityAutoConfiguration::class.java,
                    ChawpiPlatformAutoConfiguration::class.java,
                    ChawpiSecurityAutoConfiguration::class.java,
                    ChawpiMetadataAutoConfiguration::class.java,
                    ChawpiDataAutoConfiguration::class.java,
                    ChawpiAdminAutoConfiguration::class.java
                )
            ).withBean(DatabaseClient::class.java, { mock(DatabaseClient::class.java) })
            .withBean(JsonMapper::class.java, { JsonMapper.builder().build() })
            .withPropertyValues("chawpi.database.migrate=false", "chawpi.security.jwt.secret=$TEST_SECRET")
}
