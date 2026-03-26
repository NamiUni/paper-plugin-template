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
    implementation(libs.caffeine)
    implementation(libs.guice)
    compileOnly(libs.paper.api)
    compileOnly(libs.mini.placeholders)
    compileOnly(libs.configurate.yaml)
    implementation(libs.adventure.serializer.configurate)
    implementation(libs.kotonoha.message)
    implementation(libs.kotonoha.message.extra.miniplaceholders)
    implementation(libs.result4j)
}

val mainPackage = "$group.paperplugintemplate"
paperPluginYaml {
    name = "TemplatePlugin" // TODO: change the name
    author = "Namiu/Unitarou"
    website = "https://github.com/NamiUni"
    apiVersion = "1.21.11"

    main = "$mainPackage.${this.name.get()}"
    bootstrapper = "$mainPackage.TemplateBootstrap" // TODO: change the class name

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
