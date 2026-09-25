package chawpi.examples.full

import chawpi.test.ChawpiIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping

// same REST contract as the original (spec, Verification): full-sample's routes vs a snapshot of the original's,
// generated statically from its controllers by route-parity/generate-expected.sh. equal both ways, except
// KNOWN_DEVIATIONS, each one an ADR-031 entry.
class FullSampleRouteParityTest : ChawpiIntegrationTest() {
    @Autowired
    private lateinit var context: ApplicationContext

    @Test
    fun `the routes are the original's`() {
        val live = liveRoutes()
        val legacy = legacyRoutes()

        val missing = (legacy - live).map { "-$it" }
        val extra = (live - legacy).map { "+$it" }
        val unexplained = (missing + extra).filterNot { it in KNOWN_DEVIATIONS }.sorted()
        assertThat(unexplained).describedAs("routes that differ from the original with no ADR-031 entry").isEmpty()
        // a deviation that no longer happens is stale: drop it here
        assertThat(KNOWN_DEVIATIONS.keys - (missing + extra).toSet()).describedAs("stale known deviations").isEmpty()
    }

    @Test
    fun `the snapshot is the one the generator writes`() {
        // guards a hand-edited snapshot: sorted, unique, one "VERB /path" per line
        val lines = snapshot()
        assertThat(lines).isSorted.doesNotHaveDuplicates().allMatch { it.matches(Regex("(GET|POST|PUT|PATCH|DELETE) /\\S*")) }
    }

    // every annotated handler: the app's own mapping plus actuator's controller-endpoint one (both are
    // RequestMappingHandlerMapping). a mapping with no verb condition answers every verb: shown as ANY
    private fun liveRoutes(): Set<String> {
        val all =
            context
                .getBeansOfType(RequestMappingHandlerMapping::class.java)
                .values
                .flatMap { mapping ->
                    mapping.handlerMethods.keys.flatMap { info ->
                        val verbs =
                            info.methodsCondition.methods
                                .map { it.name }
                                .ifEmpty { listOf("ANY") }
                        verbs.flatMap { verb -> info.patternsCondition.patterns.map { "$verb ${it.patternString}" } }
                    }
                }.map(::normalize)
        val (skipped, kept) = all.partition(::excluded)
        // the report quotes these: what was compared and what was left out
        println("route parity: ${kept.toSet().size} live routes compared, excluded ${skipped.toSortedSet()}")
        return kept.toSet()
    }

    private fun legacyRoutes(): Set<String> = snapshot().map(::normalize).filterNot(::excluded).toSet()

    private fun snapshot(): List<String> =
        javaClass
            .getResource("/route-parity/legacy-routes.txt")!!
            .readText()
            .lines()
            .filter { it.isNotBlank() }

    companion object {
        // infra, not the REST contract: actuator (its own handler mappings anyway) and boot's error handler
        private val EXCLUDED_PREFIXES = listOf("/actuator", "/error")

        // "+VERB /path" chawpi has and the original has not, "-VERB /path" the reverse, with the ADR-031 entry
        val KNOWN_DEVIATIONS: Map<String, String> = emptyMap()

        // path variable names do not matter to a client, a trailing slash neither
        fun normalize(route: String): String {
            val (verb, path) = route.split(' ', limit = 2)
            val bare = path.replace(Regex("\\{[^}]*}"), "{}").let { if (it.length > 1) it.trimEnd('/') else it }
            return "$verb $bare"
        }

        fun excluded(route: String): Boolean {
            val path = route.substringAfter(' ')
            return EXCLUDED_PREFIXES.any { path == it || path.startsWith("$it/") }
        }
    }
}
