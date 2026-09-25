package chawpi.examples.documents

import chawpi.core.autoconfigure.ChawpiApplication
import org.springframework.boot.runApplication

// the whole server: the documents and automation starters on the classpath do the rest
@ChawpiApplication
class DocumentsSampleApplication

fun main(args: Array<String>) {
    runApplication<DocumentsSampleApplication>(*args)
}
