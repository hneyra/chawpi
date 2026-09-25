plugins {
    id("chawpi.sample-app")
}

description = "gis-sample server: chawpi core and gis, on PostGIS"

// in this repo the starters are projects. an app outside it writes the same as coordinates:
// implementation(platform("chawpi:chawpi-bom:0.1.0")) and implementation("chawpi:chawpi-spring-boot-starter-gis")
dependencies {
    implementation(platform(project(":chawpi-bom")))
    implementation(project(":chawpi-spring-boot-starter"))
    implementation(project(":chawpi-spring-boot-starter-gis"))
    testImplementation(project(":chawpi-test"))
}

// the smoke test needs PostGIS: the postgis image under testcontainers, or the external PostGIS
// server (CHAWPI_TEST_GIS_DB_PORT; database name, user and password are shared with the plain one)
val externalDb = providers.environmentVariable("CHAWPI_TEST_DB_HOST").isPresent
val gisDbPort: String? = providers.environmentVariable("CHAWPI_TEST_GIS_DB_PORT").orNull

tasks.named<Test>("integrationTest") {
    systemProperty("chawpi.test.db.image", "postgis/postgis:18-3.6")
    if (gisDbPort != null) environment("CHAWPI_TEST_DB_PORT", gisDbPort)
    val missingGisPort = externalDb && gisDbPort == null
    doFirst {
        if (missingGisPort) throw GradleException("gis-sample-server needs PostGIS: with CHAWPI_TEST_DB_HOST set, also set CHAWPI_TEST_GIS_DB_PORT")
    }
}
