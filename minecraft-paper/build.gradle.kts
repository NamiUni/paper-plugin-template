import xyz.jpenilla.resourcefactory.bukkit.Permission
import xyz.jpenilla.resourcefactory.paper.PaperPluginYaml.Load
import xyz.jpenilla.runpaper.task.RunServer

plugins {
    id("paper-plugin-template.base")
    id("paper-plugin-template.platform")
    alias(libs.plugins.resource.factory)
    alias(libs.plugins.run.paper)
}

dependencies {
    implementation(project(":paper-plugin-template-common"))
    runtimeDownload(libs.cloud.paper)
    runtimeDownload(libs.cloud.minecraft.extras)
    compileOnly(libs.paper.api)
}

val mainPackage = "${projectMetadata.group.get()}.${projectMetadata.name.get().lowercase()}"
paperPluginYaml {
    name = projectMetadata.name
    version = projectMetadata.version
    author = projectMetadata.author
    contributors = projectMetadata.contributors
    website = projectMetadata.website
    apiVersion = "1.21.11"
    foliaSupported = true

    loader = "$mainPackage.minecraft.paper.PluginLoaderImpl"
    main = "$mainPackage.minecraft.paper.JavaPluginImpl"
    bootstrapper = "$mainPackage.minecraft.paper.PluginBootstrapImpl"

    permissions {
        val prefix = paperPluginYaml.name.get().lowercase()
        register("$prefix.command.reload") {
            description = "Reloads ${projectMetadata.name}'s config."
            default = Permission.Default.OP
        }
    }

    dependencies {
        server("MiniPlaceholders", Load.BEFORE, false)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.runtimeDownload.get())
    }
}

runPaper.folia.registerTask()

tasks {
    withType(RunServer::class).configureEach {
        systemProperty("log4j.configurationFile", "log4j2.xml")
        minecraftVersion("1.21.11")
        downloadPlugins {
            url("https://ci.lucko.me/job/LuckPerms-Folia/lastSuccessfulBuild/artifact/bukkit/loader/build/libs/LuckPerms-Bukkit-${libs.versions.luckperms.get()}.jar")
            modrinth("miniplaceholders", "4zOT6txC")
            hangar("PlaceholderAPI", "2.12.2")
        }
    }
}
