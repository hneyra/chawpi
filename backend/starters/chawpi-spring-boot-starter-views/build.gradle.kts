plugins {
    id("chawpi.kotlin-library")
    id("chawpi.publishing")
}

description = "Chawpi views starter: the Chawpi starter plus chawpi-views"

dependencies {
    api(project(":chawpi-spring-boot-starter"))
    api(project(":chawpi-views"))
}
