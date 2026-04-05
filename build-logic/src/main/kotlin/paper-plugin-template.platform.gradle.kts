plugins {
    id("java-library")
    id("com.gradleup.shadow")
    id("xyz.jpenilla.gremlin-gradle")
}

dependencies {
    implementation(libs.gremlin.runtime)
}

tasks {
    shadowJar {
        archiveClassifier = null as String?

        mergeServiceFiles()
        filesMatching("META-INF/services/**") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }

        gremlin {
            listOf("xyz.jpenilla.gremlin")
                .forEach {
                    relocate(it, "libraries.$it")
                }
        }
    }

    javadoc {
        enabled = false
    }
}

gremlin {
    defaultJarRelocatorDependencies = false
    defaultGremlinRuntimeDependency = false
}
