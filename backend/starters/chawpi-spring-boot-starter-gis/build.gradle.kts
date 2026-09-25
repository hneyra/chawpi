plugins {
    id("chawpi.kotlin-library")
    id("chawpi.publishing")
}

// needs a PostgreSQL server with the postgis extension available
description = "Chawpi GIS starter: the Chawpi starter plus chawpi-gis"

dependencies {
    api(project(":chawpi-spring-boot-starter"))
    api(project(":chawpi-gis"))
}
