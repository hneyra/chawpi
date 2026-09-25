plugins {
    id("chawpi.kotlin-library")
    id("chawpi.publishing")
}

description = "Chawpi automation starter: the Chawpi starter plus chawpi-automation"

dependencies {
    api(project(":chawpi-spring-boot-starter"))
    api(project(":chawpi-automation"))
}
