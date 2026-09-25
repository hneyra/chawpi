package chawpi.examples.gis

import chawpi.core.autoconfigure.ChawpiApplication
import org.springframework.boot.runApplication

// the whole server: the gis starter on the classpath does the rest
@ChawpiApplication
class GisSampleApplication

fun main(args: Array<String>) {
    runApplication<GisSampleApplication>(*args)
}
