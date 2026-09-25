plugins {
    id("chawpi.kotlin-library")
    id("chawpi.publishing")
}

description = "Chawpi workflow starter: the Chawpi starter plus chawpi-workflow"

dependencies {
    api(project(":chawpi-spring-boot-starter"))
    api(project(":chawpi-workflow"))
}
