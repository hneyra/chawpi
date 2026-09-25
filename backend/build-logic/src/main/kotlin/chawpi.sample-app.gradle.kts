import org.springframework.boot.gradle.tasks.bundling.BootJar

// an example app: a spring boot application assembled from the chawpi starters. never published:
// chawpi-bom's constraints only take projects named chawpi-*, and this plugin refuses maven-publish.
plugins {
    id("chawpi.kotlin-library")
    id("chawpi.integration-test")
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.springframework.boot")
}

pluginManager.withPlugin("maven-publish") {
    throw GradleException("${project.path} is an example app: example apps are never published")
}

// one runnable jar with a fixed name: live boots and the playwright smoke start it without globbing
tasks.named<BootJar>("bootJar") {
    archiveFileName.set("app.jar")
}

// an app, not a library: no plain jar next to the boot jar
tasks.named<Jar>("jar") {
    enabled = false
}

// an app, not a library: no sources jar next to the boot jar either
tasks.named<Jar>("sourcesJar") {
    enabled = false
}

// an app's smoke tests are all integration-tagged: an empty unit run is expected
tasks.named<Test>("test") {
    failOnNoDiscoveredTests.set(false)
}
