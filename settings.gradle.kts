pluginManagement {
    includeBuild("backend/build-logic")
}

rootProject.name = "chawpi"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenCentral()
    }
}

// a folder is a module when it has a build file. no list to keep in sync.
fun includeModules(parent: File, nameOf: (File) -> String = { it.name }) {
    parent.listFiles()
        ?.filter { it.isDirectory && it.name != "build-logic" && File(it, "build.gradle.kts").isFile }
        ?.sortedBy { it.name }
        ?.forEach { dir ->
            val name = nameOf(dir)
            include(name)
            project(":$name").projectDir = dir
        }
}

includeModules(file("backend"))
includeModules(file("backend/starters"))
// examples/<sample>/server -> :<sample>-server
file("examples").listFiles()?.filter { it.isDirectory }?.forEach { sample ->
    includeModules(sample) { "${sample.name}-${it.name}" }
}
