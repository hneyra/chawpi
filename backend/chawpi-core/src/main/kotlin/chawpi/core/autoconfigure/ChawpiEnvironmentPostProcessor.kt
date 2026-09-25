package chawpi.core.autoconfigure

import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

// what an app gets without writing any yaml. added last: anything the app sets wins.
// no usable jwt secret on purpose (R15).
class ChawpiEnvironmentPostProcessor : EnvironmentPostProcessor {
    override fun postProcessEnvironment(
        environment: ConfigurableEnvironment,
        application: SpringApplication
    ) {
        environment.propertySources.addLast(MapPropertySource(SOURCE, DEFAULTS))
    }

    companion object {
        const val SOURCE = "chawpiDefaults"

        val DEFAULTS: Map<String, Any> =
            mapOf(
                "chawpi.database.host" to "\${CHAWPI_DB_HOST:localhost}",
                "chawpi.database.port" to "\${CHAWPI_DB_PORT:5432}",
                "chawpi.database.name" to "\${CHAWPI_DB_NAME:chawpi}",
                "chawpi.database.username" to "\${CHAWPI_DB_USERNAME:chawpi}",
                "chawpi.database.password" to "\${CHAWPI_DB_PASSWORD:chawpi}",
                "chawpi.security.jwt.secret" to "\${CHAWPI_JWT_SECRET:}",
                "spring.r2dbc.url" to "r2dbc:postgresql://\${chawpi.database.host}:\${chawpi.database.port}/\${chawpi.database.name}",
                "spring.r2dbc.username" to "\${chawpi.database.username}",
                "spring.r2dbc.password" to "\${chawpi.database.password}",
                "spring.r2dbc.pool.enabled" to "true",
                "spring.r2dbc.pool.initial-size" to "5",
                "spring.r2dbc.pool.max-size" to "20",
                "spring.webflux.problemdetails.enabled" to "true"
            )
    }
}
