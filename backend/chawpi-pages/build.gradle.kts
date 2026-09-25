plugins {
    id("chawpi.spring-module")
    id("chawpi.publishing")
    id("chawpi.integration-test")
}

description = "Chawpi pages: record pages as a component tree on a template"

dependencies {
    api(project(":chawpi-core"))
    // a FORM component names a stored form: the one hard module-to-module edge (spec module graph)
    api(project(":chawpi-forms"))

    testImplementation(project(":chawpi-test"))
}
