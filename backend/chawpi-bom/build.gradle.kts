plugins {
    `java-platform`
    id("chawpi.publishing")
}

javaPlatform {
    allowDependencies()
}

// never published: the bom itself and the test suite
// rule this filter assumes: every other chawpi-* project applies chawpi.publishing
val unpublished = setOf("chawpi-bom", "chawpi-integration-tests")

dependencies {
    api(platform(libs.spring.boot.bom))
    constraints {
        rootProject.subprojects
            .filter { it.name.startsWith("chawpi-") && it.name !in unpublished }
            .forEach { api(project(it.path)) }
    }
}
