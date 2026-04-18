plugins {
    id("com.modrinth.minotaur")
}

modrinth {
    token = providers.environmentVariable("MODRINTH_TOKEN")
    projectId = providers.gradleProperty("modrinthProjectId")
    versionType = providers.gradleProperty("modrinthVersionType").orElse("release")
    gameVersions = providers.gradleProperty("modrinthGameVersions")
        .map { it.split(",").map(String::trim).filter(String::isNotEmpty) }
    syncBodyFrom = rootProject.file("README.md").readText()
}
