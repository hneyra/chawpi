package chawpi.gis.autoconfigure

import org.springframework.boot.context.properties.ConfigurationProperties

// the module switch. geoserver publishing has its own switch: chawpi.gis.geoserver.enabled.
@ConfigurationProperties("chawpi.gis")
data class ChawpiGisProperties(
    // false: no GEOMETRY type, no bbox, no gis routes, no postgis migration
    val enabled: Boolean = true
)
