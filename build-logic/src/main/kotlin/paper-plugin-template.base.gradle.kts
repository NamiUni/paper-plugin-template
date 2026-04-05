import gradle.kotlin.dsl.accessors._27a6b18d50d34019e3acdc7613d267a2.jar

plugins {
    id("java-library")
    id("checkstyle")
    id("net.kyori.indra.licenser.spotless")
}

val config = extensions.create<ProjectMetadata>("projectMetadata")
val pluginName = config.name.convention(providers.gradleProperty("name"))
val projectVersion = config.version.convention(providers.gradleProperty("version"))
val projectGroup = config.group.convention(providers.gradleProperty("group"))
val author = config.author.convention(providers.gradleProperty("author"))
val contributors = providers.gradleProperty("contributors").getOrElse("")
    .split(", ")
    .filter { it.isNotEmpty() }
val website = config.website.convention(providers.gradleProperty("website"))

group = projectGroup
version = projectVersion

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
    property("name", pluginName)
    property("author", author)
    property("contributors", contributors.joinToString(", "))
}

dependencies {
    compileOnlyApi(libs.jspecify)
    compileOnlyApi(libs.adventure.api)
}

tasks {
    jar {
        archiveClassifier = "unshaded"
    }
    compileJava {
        options.compilerArgs.add("-parameters")
    }
}
