plugins {
    id("chawpi.kotlin-library")
    id("chawpi.publishing")
}

// chawpi-pages brings chawpi-forms with it
description = "Chawpi pages starter: the Chawpi starter plus chawpi-pages (and chawpi-forms)"

dependencies {
    api(project(":chawpi-spring-boot-starter"))
    api(project(":chawpi-pages"))
}
