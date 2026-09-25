package chawpi.examples.gis

import chawpi.test.ChawpiIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders

// the sample app itself (GisSampleApplication, above this package) on PostGIS, with no GeoServer
// anywhere: publishing is off by default, and records, features and layers work without it
class GisSampleSmokeTest : ChawpiIntegrationTest() {
    @Test
    fun `health is up without a geoserver`() {
        client
            .get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.status")
            .isEqualTo("UP")
    }

    @Test
    fun `gis answers, and the modules this sample leaves out do not`() {
        val token = bearer()
        client
            .get()
            .uri("/api/gis/layers")
            .header(HttpHeaders.AUTHORIZATION, token)
            .exchange()
            .expectStatus()
            .isOk
        listOf("/api/automation-runs", "/api/pages", "/api/agent/status").forEach { path ->
            client
                .get()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, token)
                .exchange()
                .expectStatus()
                .isNotFound
        }
    }

    @Test
    fun `a record with a polygon comes back as a feature`() {
        val token = bearer()
        val name = uniqueName("predio")
        client
            .post()
            .uri("/api/objects")
            .header(HttpHeaders.AUTHORIZATION, token)
            .bodyValue(
                mapOf(
                    "name" to name,
                    "label" to "Predio",
                    "pluralLabel" to "Predios",
                    "fields" to
                        listOf(
                            mapOf("name" to "codigo", "label" to "Codigo", "type" to "TEXT"),
                            mapOf("name" to "lote", "label" to "Lote", "type" to "GEOMETRY", "geometryType" to "POLYGON", "srid" to 4326)
                        )
                )
            ).exchange()
            .expectStatus()
            .isCreated
        val geometries = mapOf("lote" to mapOf("type" to "Polygon", "coordinates" to listOf(RING)))
        client
            .post()
            .uri("/api/objects/$name/records")
            .header(HttpHeaders.AUTHORIZATION, token)
            .bodyValue(mapOf("attributes" to mapOf("codigo" to "P-1"), "geometries" to geometries))
            .exchange()
            .expectStatus()
            .isCreated
        client
            .get()
            .uri("/api/gis/objects/$name/features")
            .header(HttpHeaders.AUTHORIZATION, token)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.type")
            .isEqualTo("FeatureCollection")
            .jsonPath("$.features.length()")
            .isEqualTo(1)
            .jsonPath("$.features[0].geometry.type")
            .isEqualTo("Polygon")
    }

    companion object {
        private val RING =
            listOf(listOf(-77.03, -12.05), listOf(-77.02, -12.05), listOf(-77.02, -12.04), listOf(-77.03, -12.04), listOf(-77.03, -12.05))
    }
}
