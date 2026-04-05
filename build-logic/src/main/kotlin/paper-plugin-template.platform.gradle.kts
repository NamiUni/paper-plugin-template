plugins {
    id("java-library")
    id("com.gradleup.shadow")
    id("xyz.jpenilla.gremlin-gradle")
}

dependencies {
    runtimeDownload(libs.h2)
    runtimeDownload(libs.mysql.connector)
}

tasks {

    shadowJar {
        archiveClassifier = null as String?
        mergeServiceFiles()
        filesMatching("META-INF/services/**") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }
    javadoc {
        enabled = false
    }
}
