plugins {
    id("chawpi.spring-module")
    id("chawpi.publishing")
    id("chawpi.integration-test")
}

description = "Chawpi documents: document types, templates and issued, numbered documents"

dependencies {
    api(project(":chawpi-core"))
    // the automation port is implemented here, but an app without automation must not get it:
    // compileOnly, and the adapter's auto-config checks the class is there
    compileOnly(project(":chawpi-automation"))

    testImplementation(project(":chawpi-test"))
    testImplementation(project(":chawpi-automation"))
}
