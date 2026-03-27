import xyz.jpenilla.resourcefactory.bukkit.Permission
import xyz.jpenilla.resourcefactory.paper.PaperPluginYaml.Load

plugins {
    id("java")
    id("checkstyle")
    alias(libs.plugins.shadow)
    alias(libs.plugins.resource.factory)
    alias(libs.plugins.run.paper)
    alias(libs.plugins.gremlin)
    alias(libs.plugins.indra.licenser.spotless)
}

group = "io.github.namiuni" // TODO: change
version = "1.0.0-SNAPSHOT"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.mini.placeholders)
    compileOnly(libs.configurate.yaml)

    runtimeDownload(libs.caffeine)
    runtimeDownload(libs.guice) {
        exclude(group = "org.checkerframework", module = "checker-qual")
    }
    runtimeDownload(libs.adventure.serializer.configurate) {
        isTransitive = false
    }
    runtimeDownload(libs.kotonoha.message) {
        exclude(group = "org.jspecify", module = "jspecify")
    }
    runtimeDownload(libs.kotonoha.message.extra.miniplaceholders) {
        exclude(group = "org.jspecify", module = "jspecify")
    }
}

val mainPackage = "$group.paperplugintemplate" // TODO: change the package
paperPluginYaml {
    name = "PaperPluginTemplate" // TODO: change the name
    author = "Namiu/Unitarou"
    website = "https://github.com/NamiUni"
    apiVersion = "1.21.11"

    main = "$mainPackage.JavaPluginImpl"
    bootstrapper = "$mainPackage.PluginBootstrapImpl"
    loader = "$mainPackage.PluginLoaderImpl"

    permissions {
        register("templateplugin.command.reload") { // TODO: change the root node
            description = "Reloads TemplatePlugin's config." // TODO: change the plugin name
            default = Permission.Default.OP
        }
    }

    dependencies {
        server("MiniPlaceholders", Load.BEFORE, false)
    }
}

indraSpotlessLicenser {
    licenseHeaderFile(rootProject.file("LICENSE_HEADER"))
    property("name", rootProject.name)
    property("author", paperPluginYaml.author)
    property("contributors", paperPluginYaml.contributors)
}

configurations {
    compileOnly {
        extendsFrom(configurations.runtimeDownload.get())
    }
}

tasks {
    checkstyle {
        toolVersion = libs.versions.check.style.get()
        configDirectory = rootDir.resolve(".checkstyle")
    }

    compileJava {
        options.compilerArgs.add("-parameters")
    }

    shadowJar {
        archiveClassifier = null as String?
        mergeServiceFiles()
    }

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
