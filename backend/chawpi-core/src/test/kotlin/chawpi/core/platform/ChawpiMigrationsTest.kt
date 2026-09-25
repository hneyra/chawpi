package chawpi.core.platform

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ChawpiMigrationsTest {
    private val database = ChawpiDatabaseProperties()
    private val schemas = ChawpiSchemas("acme_meta", "acme_data")

    @Test
    fun `modules run in order, core first, ties broken by name`() {
        val gis = ModuleMigration("gis", "classpath:db/chawpi/gis", ModuleMigration.MODULE_ORDER)
        val forms = ModuleMigration("forms", "classpath:db/chawpi/forms", ModuleMigration.MODULE_ORDER)

        val plan = ChawpiMigrations(database, schemas, listOf(gis, ModuleMigration.CORE_SEED, forms, ModuleMigration.CORE)).plan

        assertThat(plan.map { it.name }).containsExactly("core", "core_seed", "forms", "gis")
    }

    @Test
    fun `each module keeps its own history table and gets the schema placeholders`() {
        val flyway = ChawpiMigrations(database, schemas, listOf(ModuleMigration.CORE)).flyway(ModuleMigration.CORE)
        val configuration = flyway.configuration

        assertThat(configuration.table).isEqualTo("flyway_history_core")
        assertThat(configuration.defaultSchema).isEqualTo("acme_meta")
        assertThat(configuration.placeholders).containsEntry("metadataSchema", "acme_meta").containsEntry("dataSchema", "acme_data")
        assertThat(configuration.isBaselineOnMigrate).isTrue()
        assertThat(configuration.baselineVersion.version).isEqualTo("0")
        assertThat(configuration.locations.map { it.descriptor }).containsExactly("classpath:db/chawpi/core")
    }

    @Test
    fun `a module registered twice, or with an unsafe name, fails at boot`() {
        assertThatThrownBy { ChawpiMigrations(database, schemas, listOf(ModuleMigration.CORE, ModuleMigration.CORE)) }
            .isInstanceOf(IllegalStateException::class.java)
        assertThatThrownBy { ModuleMigration("core-seed", "classpath:x", 1) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
