plugins {
    id("chawpi.sample-app")
}

description = "documents-sample server: chawpi core, documents and automation"

// in this repo the starters are projects. an app outside it writes the same as coordinates:
// implementation(platform("chawpi:chawpi-bom:0.1.0")) and implementation("chawpi:chawpi-spring-boot-starter-documents")
dependencies {
    implementation(platform(project(":chawpi-bom")))
    implementation(project(":chawpi-spring-boot-starter"))
    implementation(project(":chawpi-spring-boot-starter-documents"))
    implementation(project(":chawpi-spring-boot-starter-automation"))
    testImplementation(project(":chawpi-test"))
}
