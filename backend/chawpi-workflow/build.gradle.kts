plugins {
    id("chawpi.spring-module")
    id("chawpi.publishing")
    id("chawpi.integration-test")
}

description = "Chawpi workflow: record states and transitions per object"

dependencies {
    api(project(":chawpi-core"))
    // the WORKFLOW page component, only when an app has chawpi-pages: compileOnly + @ConditionalOnClass
    compileOnly(project(":chawpi-pages"))

    testImplementation(project(":chawpi-test"))
    testImplementation(project(":chawpi-pages"))
}
