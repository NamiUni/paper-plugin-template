import xyz.jpenilla.resourcefactory.bukkit.Permission
import xyz.jpenilla.resourcefactory.paper.PaperPluginYaml.Load

plugins {
    id("java")
    alias(libs.plugins.shadow)
    alias(libs.plugins.runPaper)
    alias(libs.plugins.resouceFactory)
}

val projectVersion: String by project
version = projectVersion

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

val paperApi = libs.versions.minecraft.map { "io.papermc.paper:paper-api:$it-R0.1-SNAPSHOT" }
dependencies {
    // paper
    compileOnly(paperApi)

    // plugins
    compileOnly(libs.miniPlaceholders)

    // libraries
    implementation(libs.guice)
    implementation(libs.configurate)
    implementation(libs.adventureSerializerConfigurate4)
    implementation(libs.moonshine)

    // test
    testImplementation(paperApi)
    testImplementation(libs.junit.api)
}

val mainPackage = "$group.${rootProject.name.lowercase()}"
paperPluginYaml {
    authors = listOf("Unitarou")
    website = "https://github.com/NamiUni"
    apiVersion = "1.21"

    main = "$mainPackage.${rootProject.name}"
    bootstrapper = "$mainPackage.PaperBootstrap"

    permissions {
        register("${rootProject.name.lowercase()}.command.reload") {
            description = "Reloads ${rootProject.name}'s config."
            default = Permission.Default.OP
        }
    }

    dependencies {
        server("MiniPlaceholders", Load.BEFORE, false)
        server("LuckPerms", Load.BEFORE, true)
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    test {
        useJUnitPlatform()
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }

    shadowJar {
        archiveClassifier = null as String?
        archiveVersion = paperPluginYaml.version

        listOf(
            "com.google.inject",
            "org.spongepowered.configurate",
            "net.kyori.adventure.serializer",
            "net.kyori.moonshine",
        ).forEach {
            relocate(it, "$mainPackage.libs.$it")
        }
    }

    runServer {
        systemProperty("com.mojang.eula.agree", "true")
        minecraftVersion(libs.versions.minecraft.get())
        downloadPlugins {
            url("https://download.luckperms.net/1552/bukkit/loader/LuckPerms-Bukkit-5.4.137.jar")

//            github("MiniPlaceholders", "MiniPlaceholders", "2.2.4", "MiniPlaceholders-Paper-2.2.4.jar")
            github("MiniPlaceholders", "PlaceholderAPI-Expansion", "1.2.0", "PlaceholderAPI-Expansion-1.2.0.jar")
            hangar("PlaceholderAPI", "2.11.6")
        }
    }
}
