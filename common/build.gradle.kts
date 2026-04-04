plugins {
    id("paper-plugin-template.base")
}

dependencies {
    api(project(":paper-plugin-template-api"))
    api(libs.gson)
    api(libs.guice)

    compileOnlyApi(libs.adventure.text.logger.slf4j)
    compileOnlyApi(libs.mini.placeholders)
    api(libs.configurate.yaml)
    api(libs.adventure.serializer.configurate) {
        isTransitive = false
    }

    // Localizations
    api(libs.caffeine)
    api(libs.kotonoha.message)
    api(libs.kotonoha.message.extra.miniplaceholders)

    // Commands
    compileOnlyApi(libs.cloud.core)

    // Storage
    api(libs.jdbi.core)
    api(libs.jdbi.sqlobject)
    api(libs.jdbi.caffeine.cache)
//  api(libs.jdbi.guice)
    api(libs.hikari)
}
