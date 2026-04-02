import xyz.jpenilla.resourcefactory.bukkit.Permission
import xyz.jpenilla.resourcefactory.paper.PaperPluginYaml.Load

plugins {
    id("paper-plugin-template.base")
    id("paper-plugin-template.platform")
    alias(libs.plugins.resource.factory)
    alias(libs.plugins.run.paper)
}

dependencies {
    implementation(project(":paper-plugin-template-common"))
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

    loader = "$mainPackage.PluginLoaderImpl"
    main = "$mainPackage.JavaPluginImpl"
    bootstrapper = "$mainPackage.PluginBootstrapImpl"

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

tasks {
    runServer {
        systemProperty("log4j.configurationFile", "log4j2.xml")
        minecraftVersion("1.21.11")
        downloadPlugins {
            modrinth("luckperms", "v5.5.0-bukkit")
            modrinth("miniplaceholders", "4zOT6txC")
            hangar("PlaceholderAPI", "2.12.2")
        }
    }
}
