import gradle.kotlin.dsl.accessors._27a6b18d50d34019e3acdc7613d267a2.jar

plugins {
    id("java-library")
    id("checkstyle")
    id("net.kyori.indra.licenser.spotless")
    id("paper-plugin-template.metadata")
}

val metadata = extensions.getByType<ProjectMetadata>()

group = metadata.projectGroup.get()
version = metadata.projectVersion.get()

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    configDirectory = rootProject.file(".checkstyle")
}

indraSpotlessLicenser {
    licenseHeaderFile(rootProject.file("LICENSE_HEADER"))
    property("name", metadata.projectName)
    property("author", metadata.authorName)
    property("contributors", metadata.projectContributors.getOrElse("[]"))
}

tasks {
    jar {
        archiveClassifier = "unshaded"
    }
    compileJava {
        options.compilerArgs.add("-parameters")
    }
}
