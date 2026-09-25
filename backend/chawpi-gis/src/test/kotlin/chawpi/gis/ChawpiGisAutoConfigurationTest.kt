package chawpi.gis

import chawpi.core.data.RecordQueryParser
import chawpi.core.metadata.FieldTypeRegistry
import chawpi.core.metadata.ObjectRemovalListener
import chawpi.core.platform.ModuleMigration
import chawpi.gis.autoconfigure.ChawpiGisAutoConfiguration
import chawpi.test.ChawpiContextRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.context.annotation.ImportCandidates

class ChawpiGisAutoConfigurationTest {
    private val runner = ChawpiContextRunner.core().withConfiguration(AutoConfigurations.of(ChawpiGisAutoConfiguration::class.java))

    @Test
    fun `gis plugs GEOMETRY, bbox and layer cleanup into core`() {
        runner.run { context ->
            assertThat(context).hasNotFailed()
            val registry = context.getBean(FieldTypeRegistry::class.java)
            assertThat(registry.types.last()).isEqualTo(GEOMETRY)
            assertThat(registry.sections).containsExactly("geometries")
            val query = context.getBean(RecordQueryParser::class.java).parse(mapOf("bbox" to "1,2,3,4", "codigo" to "A-1"))
            assertThat(query.filters).containsOnlyKeys("codigo")
            assertThat(query.criteria).hasSize(1)
            assertThat(context.getBeansOfType(ObjectRemovalListener::class.java).values).hasAtLeastOneElementOfType(LayerCleanup::class.java)
            assertThat(context).hasSingleBean(FeatureController::class.java)
            assertThat(context).hasSingleBean(LayerController::class.java)
            assertThat(context.getBeansOfType(ModuleMigration::class.java).values.map { it.name }).containsExactlyInAnyOrder("core", "gis")
        }
    }

    @Test
    fun `geoserver settings bind under chawpi gis geoserver`() {
        runner.withPropertyValues("chawpi.gis.geoserver.url=http://gs:8080/geoserver/", "chawpi.gis.geoserver.enabled=false").run { context ->
            val properties = context.getBean(GeoServerProperties::class.java)
            assertThat(properties.baseUrl).isEqualTo("http://gs:8080/geoserver")
            assertThat(properties.enabled).isFalse()
            // unset: the geoserver client falls back to chawpi.database.data-schema, not a literal default
            assertThat(properties.datastore.schema).isNull()
            assertThat(properties.workspace).isEqualTo("chawpi")
        }
    }

    // an app that only sets chawpi.database.data-schema still gets a working layer: no separate
    // geoserver override is required
    @Test
    fun `data-schema flows to geoserver when no datastore override is set`() {
        runner.withPropertyValues("chawpi.database.data-schema=acme_data").run { context ->
            assertThat(context).hasNotFailed()
            assertThat(context.getBean(GeoServerClient::class.java).schema).isEqualTo("acme_data")
        }
    }

    // an explicit datastore override wins over chawpi.database.data-schema: geoserver may read a
    // different schema than the one chawpi itself writes to
    @Test
    fun `a datastore schema override wins over data-schema`() {
        runner
            .withPropertyValues("chawpi.database.data-schema=acme_data", "chawpi.gis.geoserver.datastore.schema=layers_src")
            .run { context ->
                assertThat(context).hasNotFailed()
                assertThat(context.getBean(GeoServerClient::class.java).schema).isEqualTo("layers_src")
            }
    }

    // the core-only answer: no GEOMETRY, and ?bbox= is just an unknown field filter (P1 R7)
    @Test
    fun `switched off, core knows nothing of geometry`() {
        runner.withPropertyValues("chawpi.gis.enabled=false").run { context ->
            assertThat(context).hasNotFailed()
            assertThat(context.getBean(FieldTypeRegistry::class.java).isInstalled(GEOMETRY)).isFalse()
            assertThat(context.getBean(RecordQueryParser::class.java).parse(mapOf("bbox" to "1,2,3,4")).filters).containsOnlyKeys("bbox")
            assertThat(context).doesNotHaveBean(FeatureController::class.java)
            assertThat(context.getBeansOfType(ModuleMigration::class.java).values.map { it.name }).containsExactly("core")
        }
    }

    @Test
    fun `the imports file registers the auto-config`() {
        assertThat(ImportCandidates.load(AutoConfiguration::class.java, javaClass.classLoader).candidates)
            .contains("chawpi.gis.autoconfigure.ChawpiGisAutoConfiguration")
    }
}
