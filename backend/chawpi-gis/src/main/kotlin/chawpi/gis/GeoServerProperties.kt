package chawpi.gis

import org.springframework.boot.context.properties.ConfigurationProperties

// how geoserver itself reaches postgres. not how chawpi reaches it: geoserver lives in another container.
data class GeoServerDataStoreProperties(
    val name: String = "chawpi-postgis",
    val host: String = "postgres",
    val port: Int = 5432,
    val database: String = "chawpi",
    // null: follow chawpi.database.data-schema, so an app that only sets that still gets a working layer
    val schema: String? = null,
    val username: String = "chawpi",
    val password: String = "chawpi"
)

@ConfigurationProperties(prefix = "chawpi.gis.geoserver")
data class GeoServerProperties(
    val url: String = "http://localhost:8081/geoserver",
    val username: String = "admin",
    val password: String = "geoserver",
    val workspace: String = "chawpi",
    val enabled: Boolean = true,
    val datastore: GeoServerDataStoreProperties = GeoServerDataStoreProperties()
) {
    // a trailing slash would double up in every rest path
    val baseUrl: String get() = url.trimEnd('/')
}
