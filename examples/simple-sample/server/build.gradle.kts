plugins {
    id("chawpi.sample-app")
}

description = "simple-sample server: chawpi core alone, on plain PostgreSQL"

// in this repo the starters are projects. an app outside it writes the same as coordinates:
// implementation(platform("chawpi:chawpi-bom:0.1.0")) and implementation("chawpi:chawpi-spring-boot-starter")
dependencies {
    implementation(platform(project(":chawpi-bom")))
    implementation(project(":chawpi-spring-boot-starter"))
    testImplementation(project(":chawpi-test"))
}
