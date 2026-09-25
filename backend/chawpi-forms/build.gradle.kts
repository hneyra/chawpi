plugins {
    id("chawpi.spring-module")
    id("chawpi.publishing")
    id("chawpi.integration-test")
}

description = "Chawpi forms: named, sectioned forms per object"

dependencies {
    api(project(":chawpi-core"))

    testImplementation(project(":chawpi-test"))
}
