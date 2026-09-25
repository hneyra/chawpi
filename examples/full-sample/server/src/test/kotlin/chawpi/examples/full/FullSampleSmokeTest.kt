package chawpi.examples.full

import chawpi.test.ChawpiIntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders

// the sample app itself (FullSampleApplication, above this package) with every starter, on PostGIS,
// no GeoServer and no ANTHROPIC_API_KEY: every module is there and none of them needs the extras
class FullSampleSmokeTest : ChawpiIntegrationTest() {
    private lateinit var token: String
    private lateinit var objectName: String

    @BeforeEach
    fun predio() {
        token = bearer()
        objectName = uniqueName("predio")
        val fields = listOf(mapOf("name" to "codigo", "type" to "TEXT"))
        post("/api/objects", mapOf("name" to objectName, "label" to "Predio", "pluralLabel" to "Predios", "fields" to fields))
    }

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
    fun `every module answers`() {
        listOf(
            "/api/pages",
            "/api/metadata/page-templates",
            "/api/automation-runs",
            "/api/gis/layers",
            "/api/agent/status",
            "/api/objects/$objectName/views",
            "/api/objects/$objectName/forms",
            "/api/objects/$objectName/document-types"
        ).forEach { path ->
            client
                .get()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, token)
                .exchange()
                .expectStatus()
                .isOk
        }
    }

    @Test
    fun `a record moves through its workflow`() {
        val transitionDefs = listOf(mapOf("name" to "approve", "label" to "Aprobar", "from" to "draft", "to" to "approved", "roles" to emptyList<String>()))
        client
            .put()
            .uri("/api/objects/$objectName/workflow")
            .header(HttpHeaders.AUTHORIZATION, token)
            .bodyValue(
                mapOf(
                    "name" to uniqueName("wf"),
                    "label" to "Aprobacion",
                    "enabled" to true,
                    "definition" to
                        mapOf(
                            "states" to
                                listOf(
                                    mapOf("name" to "draft", "label" to "Borrador", "type" to "INITIAL"),
                                    mapOf("name" to "approved", "label" to "Aprobado", "type" to "FINAL")
                                ),
                            "transitions" to transitionDefs
                        )
                )
            ).exchange()
            .expectStatus()
            .isOk
        val body = post("/api/objects/$objectName/records", mapOf("attributes" to mapOf("codigo" to "P-1")))
        val record = Regex("\"id\":\"([^\"]+)\"").find(body)!!.groupValues[1]

        transitions(record).jsonPath("$[0].name").isEqualTo("approve")
        client
            .post()
            .uri("/api/objects/$objectName/records/$record/transitions/approve")
            .header(HttpHeaders.AUTHORIZATION, token)
            .exchange()
            .expectStatus()
            .is2xxSuccessful
        // approved is FINAL: nothing leaves it
        transitions(record).jsonPath("$.length()").isEqualTo(0)
    }

    private fun transitions(record: String) =
        client
            .get()
            .uri("/api/objects/$objectName/records/$record/transitions")
            .header(HttpHeaders.AUTHORIZATION, token)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()

    private fun post(
        uri: String,
        body: Map<String, Any?>
    ): String =
        client
            .post()
            .uri(uri)
            .header(HttpHeaders.AUTHORIZATION, token)
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody()
            .returnResult()
            .responseBody
            ?.decodeToString()
            .orEmpty()
}
