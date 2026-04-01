@file:Suppress("UnstableApiUsage")

rootProject.name = "paper-plugin-template" // TODO: change

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

pluginManagement {
    includeBuild("build-logic")
}

sequenceOf(
    "api",
    "common",
    "minecraft-paper"
).forEach {
    include("${rootProject.name}-$it")
    project(":${rootProject.name}-$it").projectDir = file(it)
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
