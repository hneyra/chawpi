plugins {
    id("chawpi.spring-module")
    id("chawpi.publishing")
    id("chawpi.integration-test")
}

description = "Chawpi automation: trigger -> conditions -> actions on record changes"

dependencies {
    api(project(":chawpi-core"))

    testImplementation(project(":chawpi-test"))
}
