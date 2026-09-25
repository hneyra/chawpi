package chawpi.examples.documents

import chawpi.test.ChawpiIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import tools.jackson.databind.json.JsonMapper
import java.time.Duration

// the sample app itself (DocumentsSampleApplication, above this package): core, documents and
// automation. plain postgres is enough; the automation drain runs as it does in the real app.
class DocumentsSampleSmokeTest : ChawpiIntegrationTest() {
    private lateinit var token: String
    private lateinit var objectName: String
    private lateinit var prefix: String

    @BeforeEach
    fun predio() {
        token = bearer()
        objectName = uniqueName("predio")
        prefix = uniqueName("s").uppercase().take(10)
        val fields = listOf(mapOf("name" to "codigo", "type" to "TEXT"))
        post("/api/objects", mapOf("name" to objectName, "label" to "Predio", "pluralLabel" to "Predios", "fields" to fields))
        post(
            "/api/objects/$objectName/document-types",
            mapOf("name" to "oficio", "prefix" to prefix, "label" to "Oficio", "template" to doc(paragraph(text("Oficio")), paragraph(field("codigo"))))
        )
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
    fun `automation answers, and the modules this sample leaves out do not`() {
        client
            .get()
            .uri("/api/automation-runs")
            .header(HttpHeaders.AUTHORIZATION, token)
            .exchange()
            .expectStatus()
            .isOk
        listOf("/api/gis/layers", "/api/pages", "/api/agent/status").forEach { path ->
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
    fun `a record issues a document by hand and the issue lands in its history`() {
        val record = idOf(post("/api/objects/$objectName/records", mapOf("attributes" to mapOf("codigo" to "P-1"))))
        client
            .post()
            .uri("/api/objects/$objectName/records/$record/documents/oficio")
            .header(HttpHeaders.AUTHORIZATION, token)
            .exchange()
            .expectStatus()
            .isCreated
            .expectBody()
            .jsonPath("$.status")
            .isEqualTo("VALID")
        client
            .get()
            .uri("/api/objects/$objectName/records/$record/history")
            .header(HttpHeaders.AUTHORIZATION, token)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$[0].operation")
            .isEqualTo("ISSUE")
    }

    // documents and automation meet only through automation's optional DocumentIssuer port:
    // this sample has both, so a GENERATE_DOCUMENT action issues for real
    @Test
    fun `an automation issues a document when a record is created`() {
        post(
            "/api/objects/$objectName/automations",
            mapOf(
                "name" to uniqueName("auto"),
                "label" to "Oficio al crear",
                "definition" to
                    mapOf(
                        "trigger" to mapOf("type" to "RECORD_CREATED"),
                        "actions" to listOf(mapOf("type" to "GENERATE_DOCUMENT", "documentType" to "oficio"))
                    )
            )
        )
        val record = idOf(post("/api/objects/$objectName/records", mapOf("attributes" to mapOf("codigo" to "P-2"))))

        // the background drain polls every second
        val deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos()
        var issued = documentsOf(record)
        while (issued == 0 && System.nanoTime() < deadline) {
            Thread.sleep(250)
            issued = documentsOf(record)
        }
        assertThat(issued).isEqualTo(1)
    }

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

    private fun idOf(body: String): String = Regex("\"id\":\"([^\"]+)\"").find(body)!!.groupValues[1]

    private val jsonMapper = JsonMapper.builder().build()

    private fun documentsOf(record: String): Int {
        val body =
            client
                .get()
                .uri("/api/objects/$objectName/records/$record/documents")
                .header(HttpHeaders.AUTHORIZATION, token)
                .exchange()
                .expectStatus()
                .isOk
                .expectBody()
                .returnResult()
                .responseBody
                ?.decodeToString()
                .orEmpty()
        // the endpoint returns a JSON array: count its elements instead of grepping field names,
        // which double-counts "number" (it repeats in each document's frozen snapshot, by design).
        return jsonMapper.readTree(body).size()
    }

    private fun doc(vararg content: Map<String, Any?>): Map<String, Any?> = mapOf("type" to "doc", "content" to content.toList())

    private fun paragraph(vararg content: Map<String, Any?>): Map<String, Any?> = mapOf("type" to "paragraph", "content" to content.toList())

    private fun text(value: String): Map<String, Any?> = mapOf("type" to "text", "text" to value)

    private fun field(name: String): Map<String, Any?> = mapOf("type" to "objectField", "attrs" to mapOf("field" to name))
}
