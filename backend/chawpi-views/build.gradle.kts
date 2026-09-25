plugins {
    id("chawpi.spring-module")
    id("chawpi.publishing")
    id("chawpi.integration-test")
}

description = "Chawpi views: named list views per object"

dependencies {
    api(project(":chawpi-core"))

    testImplementation(project(":chawpi-test"))
}
