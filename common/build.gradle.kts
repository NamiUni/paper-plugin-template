plugins {
    id("paper-plugin-template.base")
}

dependencies {
    api(project(":paper-plugin-template-api"))
    api(libs.gson)
    api(libs.guice)

    compileOnlyApi(libs.adventure.text.logger.slf4j)
    compileOnlyApi(libs.mini.placeholders)
    api(libs.configurate.hocon)
    api(libs.adventure.serializer.configurate) {
        isTransitive = false
    }

    // Localizations
    api(libs.caffeine)
    api(libs.kotonoha.message)
    api(libs.kotonoha.message.extra.miniplaceholders)

    // Commands
    compileOnlyApi(libs.cloud.core)
    compileOnlyApi(libs.cloud.minecraft.extras)

    // Storage
    api(libs.jdbi.core)
    api(libs.jdbi.postgres)
    api(libs.jdbi.sqlobject)
    api(libs.jdbi.caffeine.cache)
    api(libs.hikari)

    // Database migrations
    api(libs.flyway.core)
    // flyway-mysql is required at runtime for MySQL / H2-in-MySQL-mode dialect support
    runtimeOnly(libs.flyway.mysql)
    runtimeOnly(libs.flyway.postgresql)
    runtimeOnly(libs.postgresql)
}
