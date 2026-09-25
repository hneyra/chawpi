package chawpi.examples.simple

import chawpi.test.ChawpiIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders

// the sample app itself (SimpleSampleApplication, the @SpringBootConfiguration above this package)
// on plain PostgreSQL: core alone boots, and every optional module is simply absent
class SimpleSampleSmokeTest : ChawpiIntegrationTest() {
    @Test
    fun `health is up`() {
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
    fun `the seeded admin signs in and creates a plain object`() {
        client
            .post()
            .uri("/api/objects")
            .header(HttpHeaders.AUTHORIZATION, bearer())
            .bodyValue(mapOf("name" to uniqueName("simple"), "label" to "Simple", "fields" to listOf(mapOf("name" to "codigo", "type" to "TEXT"))))
            .exchange()
            .expectStatus()
            .isCreated
    }

    @Test
    fun `no optional module answers`() {
        val token = bearer()
        listOf("/api/gis/layers", "/api/automation-runs", "/api/pages", "/api/agent/status").forEach { path ->
            client
                .get()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, token)
                .exchange()
                .expectStatus()
                .isNotFound
        }
    }

    // gis optional: without its module a GEOMETRY field is a client error, never a 500
    @Test
    fun `a GEOMETRY field is refused without the gis module`() {
        client
            .post()
            .uri("/api/objects")
            .header(HttpHeaders.AUTHORIZATION, bearer())
            .bodyValue(
                mapOf(
                    "name" to uniqueName("nogis"),
                    "label" to "Sin GIS",
                    "fields" to listOf(mapOf("name" to "lote", "type" to "GEOMETRY", "geometryType" to "POLYGON", "srid" to 4326))
                )
            ).exchange()
            .expectStatus()
            .isBadRequest
    }
}
