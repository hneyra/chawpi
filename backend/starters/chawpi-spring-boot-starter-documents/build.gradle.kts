plugins {
    id("chawpi.kotlin-library")
    id("chawpi.publishing")
}

// automation can issue documents when an app adds chawpi-spring-boot-starter-automation too
description = "Chawpi documents starter: the Chawpi starter plus chawpi-documents"

dependencies {
    api(project(":chawpi-spring-boot-starter"))
    api(project(":chawpi-documents"))
}
