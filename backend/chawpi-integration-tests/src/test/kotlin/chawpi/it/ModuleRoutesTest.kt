package chawpi.it

import chawpi.it.support.ModuleRoutes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ModuleRoutesTest {
    @Test
    fun `the matrix probes exactly the module routes the wiring test pins`() {
        assertThat(ModuleRoutes.all).doesNotHaveDuplicates().containsExactlyInAnyOrderElementsOf(AllModulesWiringTest.LEGACY_MODULE_ROUTES)
    }

    @Test
    fun `a probe fills every path variable`() {
        ModuleRoutes.all.forEach { route -> assertThat(ModuleRoutes.probe(route, "predio").second).describedAs(route).doesNotContain("{", "}") }
    }
}
