package chawpi.examples.full

import chawpi.core.autoconfigure.ChawpiApplication
import org.springframework.boot.runApplication

// the whole server: every starter on the classpath does the rest
@ChawpiApplication
class FullSampleApplication

fun main(args: Array<String>) {
    runApplication<FullSampleApplication>(*args)
}
