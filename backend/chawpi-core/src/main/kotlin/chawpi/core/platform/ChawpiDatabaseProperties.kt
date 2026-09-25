package chawpi.core.platform

import org.springframework.boot.context.properties.ConfigurationProperties

// one place for db coords. r2dbc at runtime, jdbc once for flyway.
@ConfigurationProperties(prefix = "chawpi.database")
data class ChawpiDatabaseProperties(
    val host: String = "localhost",
    val port: Int = 5432,
    val name: String = "chawpi",
    val username: String = "chawpi",
    val password: String = "chawpi",
    val metadataSchema: String = "chawpi",
    val dataSchema: String = "app_data",
    // false when the app runs the migrations some other way
    val migrate: Boolean = true
) {
    val jdbcUrl: String
        get() = "jdbc:postgresql://$host:$port/$name"
}
