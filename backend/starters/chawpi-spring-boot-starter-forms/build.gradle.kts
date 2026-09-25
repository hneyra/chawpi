plugins {
    id("chawpi.kotlin-library")
    id("chawpi.publishing")
}

description = "Chawpi forms starter: the Chawpi starter plus chawpi-forms"

dependencies {
    api(project(":chawpi-spring-boot-starter"))
    api(project(":chawpi-forms"))
}
