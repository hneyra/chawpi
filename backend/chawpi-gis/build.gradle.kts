plugins {
    id("chawpi.spring-module")
    id("chawpi.publishing")
    id("chawpi.integration-test")
}

description = "Chawpi GIS: the GEOMETRY field type on PostGIS, bbox queries, features and GeoServer layers"

dependencies {
    api(project(":chawpi-core"))
    // the MAP page component, only when an app has chawpi-pages: compileOnly + @ConditionalOnClass
    compileOnly(project(":chawpi-pages"))

    testImplementation(project(":chawpi-test"))
    testImplementation(project(":chawpi-pages"))
}
