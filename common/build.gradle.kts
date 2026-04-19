plugins {
    id("paper-plugin-template.base")
    id("paper-plugin-template.testing")
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
    annotationProcessor(libs.kotonoha.resourcebundle.generator.processor)

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

    // Testing
    testImplementation(libs.adventure.api)
    testImplementation(libs.adventure.text.minimessage)
    testImplementation(libs.adventure.text.logger.slf4j)
    testImplementation(libs.caffeine)
    testImplementation(libs.gson)
    testImplementation(libs.guice)
    testImplementation(libs.configurate.hocon)
    testImplementation(libs.adventure.serializer.configurate)
    testImplementation(libs.guice.testlib)
    testImplementation(libs.cloud.core)
    testImplementation(libs.jdbi.core)
    testImplementation(libs.jdbi.sqlobject)
    testImplementation(libs.jdbi.caffeine.cache)
    testImplementation(libs.jdbi.testing)
    testImplementation(libs.h2)
}
