plugins {
    id("paper-plugin-template.base")
}

dependencies {
    api(project(":paper-plugin-template-api"))

    // Gson
    compileOnlyApi(libs.gson)

    // Adventure
    compileOnlyApi(libs.adventure.text.logger.slf4j)
    compileOnlyApi(libs.adventure.text.minimessage)
    compileOnlyApi(libs.mini.placeholders)

    // DI
    compileOnlyApi(libs.guice)

    // Config
    compileOnlyApi(libs.configurate.hocon)
    compileOnlyApi(libs.adventure.serializer.configurate)

    // Cache
    compileOnlyApi(libs.caffeine)

    // i18n
    compileOnlyApi(libs.kotonoha.message)
    compileOnlyApi(libs.kotonoha.message.extra.miniplaceholders)

    // Commands
    compileOnlyApi(libs.cloud.core)
    compileOnlyApi(libs.cloud.minecraft.extras)

    // Storage
    compileOnlyApi(libs.jdbi.core)
    compileOnlyApi(libs.jdbi.postgres)
    compileOnlyApi(libs.jdbi.sqlobject)
    compileOnlyApi(libs.jdbi.caffeine.cache)
    compileOnlyApi(libs.hikari)
    compileOnlyApi(libs.flyway.core)
}
