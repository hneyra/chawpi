package chawpi.examples.simple

import chawpi.core.autoconfigure.ChawpiApplication
import org.springframework.boot.runApplication

// the whole server: the starter on the classpath does the rest
@ChawpiApplication
class SimpleSampleApplication

fun main(args: Array<String>) {
    runApplication<SimpleSampleApplication>(*args)
}
