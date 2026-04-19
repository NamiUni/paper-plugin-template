import xyz.jpenilla.resourcefactory.bukkit.Permission
import xyz.jpenilla.resourcefactory.paper.PaperPluginYaml.Load
import xyz.jpenilla.runpaper.task.RunServer

plugins {
    id("paper-plugin-template.base")
    id("paper-plugin-template.platform")
    id("paper-plugin-template.modrinth")
    id("xyz.jpenilla.run-paper")
    alias(libs.plugins.resource.factory)
}

dependencies {
    implementation(project(":paper-plugin-template-common"))

    // Paper API
    compileOnly(libs.paper.api)

    // DI
    runtimeDownload(libs.guice)

    // Config
    runtimeDownload(libs.configurate.hocon) { isTransitive = false }
    runtimeDownload(libs.adventure.serializer.configurate) { isTransitive = false }

    // Cache
    runtimeDownload(libs.caffeine) { isTransitive = false }

    // i18n
    runtimeDownload(libs.kotonoha.annotations) { isTransitive = false }
    runtimeDownload(libs.kotonoha.message) { isTransitive = false }
    runtimeDownload(libs.kotonoha.message.extra.miniplaceholders) { isTransitive = false }

    // Command
    compileOnly(libs.cloud.paper)
    runtimeDownload(libs.cloud.paper)
    runtimeDownload(libs.cloud.minecraft.extras) { isTransitive = false }

    // Storage
    runtimeDownload(libs.jdbi.core) { isTransitive = false }
    runtimeDownload(libs.jdbi.postgres) { isTransitive = false }
    runtimeDownload(libs.jdbi.caffeine.cache) { isTransitive = false }
    runtimeDownload(libs.hikari) { isTransitive = false }

    // Flyway
    runtimeDownload(libs.flyway.core) {
        exclude("com.fasterxml.jackson.core", "jackson-core")
        exclude("tools.jackson", "jackson-bom")
    }
    runtimeDownload(libs.flyway.mysql) { isTransitive = false }
    runtimeDownload(libs.flyway.postgresql) { isTransitive = false }

    // JDBC Drivers
    runtimeDownload(libs.postgresql) { isTransitive = false }
    runtimeDownload(libs.h2)
    runtimeDownload(libs.mysql.connector)

    // bstats
    implementation(libs.bstats.bukkit)
}

val mainPackage = "${projectMetadata.projectGroup.get()}.${projectMetadata.projectName.get().lowercase()}"
paperPluginYaml {
    name = projectMetadata.projectName
    version = projectMetadata.projectVersion
    description = projectMetadata.projectDescription
    author = projectMetadata.authorName
    contributors = projectMetadata.projectContributors.getOrElse("")
        .split(", ")
        .filter { it.isNotEmpty() }
    website = projectMetadata.projectWebsite
    apiVersion = "26.1.2"

    loader = "$mainPackage.minecraft.paper.PaperLoader"
    main = "$mainPackage.minecraft.paper.PaperPlugin"
    bootstrapper = "$mainPackage.minecraft.paper.PaperBootstrap"

    permissions {
        val prefix = projectMetadata.projectName.get().lowercase()
        register("$prefix.command.reload") {
            description = "Reloads ${projectMetadata.projectName}'s config."
            default = Permission.Default.OP
        }
        register("$prefix.command.help") {
            description = "Displays help for ${projectMetadata.projectName}'s commands."
            default = Permission.Default.TRUE
        }
    }

    dependencies {
        server("MiniPlaceholders", Load.BEFORE, false)
    }
}

modrinth {
    loaders = providers.gradleProperty("modrinthLoaders")
        .map { it.split(",").map(String::trim).filter(String::isNotEmpty) }
        .orElse(listOf("paper", "folia"))

    uploadFile.set(tasks.shadowJar)
}

runPaper.folia.registerTask()

tasks {
    withType(RunServer::class).configureEach {
        systemProperty("log4j.configurationFile", "log4j2.xml")
        minecraftVersion("26.1.2")
        downloadPlugins {
            url("https://ci.lucko.me/job/LuckPerms-Folia/lastSuccessfulBuild/artifact/bukkit/loader/build/libs/LuckPerms-Bukkit-${libs.versions.luckperms.get()}.jar")
            modrinth("miniplaceholders", "4zOT6txC")
            hangar("PlaceholderAPI", "2.12.2")
        }
    }
}

configurations.runtimeDownload {
    exclude("org.checkerframework", "checker-qual")
    exclude("com.google.guava")
    exclude("com.google.errorprone", "error_prone_annotations")
}
