plugins {
    id("chawpi.spring-module")
    id("chawpi.publishing")
    id("chawpi.integration-test")
}

description = "Chawpi core: metadata, records, identity, organizations, audit"

dependencies {
    // an app on chawpi-core is a webflux + r2dbc + jwt app. these are part of the api.
    api(libs.spring.boot.starter.webflux)
    api(libs.spring.boot.starter.data.r2dbc)
    api(libs.spring.boot.starter.security)
    api(libs.spring.boot.starter.oauth2.resource.server)
    api(libs.spring.boot.starter.validation)
    api(libs.jackson.module.kotlin)
    api(libs.kotlinx.coroutines.reactor)
    // flyway runs over jdbc at startup (ADR-008). drivers come with the starter (P2).
    implementation(libs.flyway.core)

    testImplementation(libs.spring.boot.starter.webflux.test)
    testImplementation(project(":chawpi-test"))
    testRuntimeOnly(libs.r2dbc.postgresql)
    testRuntimeOnly(libs.postgresql.jdbc)
    testRuntimeOnly(libs.flyway.postgresql)
}
