plugins {
    id("paper-plugin-template.base")
}

dependencies {
    implementation(project(":paper-plugin-template-common"))

    // Sponge API
    compileOnly(libs.sponge.api)

    // DI
    implementation(libs.guice)

    // Config
    implementation(libs.configurate.hocon) { isTransitive = false }
    implementation(libs.adventure.serializer.configurate) { isTransitive = false }

    // Cache
    implementation(libs.caffeine) { isTransitive = false }

    // i18n
    implementation(libs.kotonoha.annotations) { isTransitive = false }
    implementation(libs.kotonoha.message) { isTransitive = false }
    implementation(libs.kotonoha.message.extra.miniplaceholders) { isTransitive = false }

    // Command
    compileOnly(libs.cloud.sponge)
    implementation(libs.cloud.sponge)
    implementation(libs.cloud.minecraft.extras) { isTransitive = false }

    // Storage
    implementation(libs.jdbi.core) { isTransitive = false }
    implementation(libs.jdbi.postgres) { isTransitive = false }
    implementation(libs.jdbi.sqlobject) { isTransitive = false }
    implementation(libs.jdbi.caffeine.cache) { isTransitive = false }
    implementation(libs.hikari) { isTransitive = false }

    // Flyway
    implementation(libs.flyway.core) {
        exclude("com.fasterxml.jackson.core", "jackson-core")
        exclude("tools.jackson", "jackson-bom")
    }
    implementation(libs.flyway.mysql) { isTransitive = false }
    implementation(libs.flyway.postgresql) { isTransitive = false }

    // JDBC Drivers
    implementation(libs.postgresql) { isTransitive = false }
    implementation(libs.h2)
    implementation(libs.mysql.connector)

    // bstats
    implementation(libs.bstats.sponge)
}

tasks.processResources {
    val props = mapOf(
        "id" to projectMetadata.projectName.get().lowercase(),
        "name" to projectMetadata.projectName.get(),
        "version" to projectMetadata.projectVersion.get(),
        "description" to projectMetadata.projectDescription.get(),
        "website" to projectMetadata.projectWebsite.get(),
        "authorName" to projectMetadata.authorName.get(),
        "mainClass" to "${projectMetadata.projectGroup.get()}.${projectMetadata.projectName.get().lowercase()}" +
                ".minecraft.sponge.SpongePlugin"
    )
    inputs.properties(props)
    filesMatching("META-INF/sponge_plugins.json") {
        expand(props)
    }
}

configurations.implementation {
    exclude("org.checkerframework", "checker-qual")
    exclude("com.google.guava")
    exclude("com.google.errorprone", "error_prone_annotations")
}
