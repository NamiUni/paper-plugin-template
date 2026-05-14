// Note: AIに書かせたからよく分かってない。あとでちゃんと書くかも。
import java.security.MessageDigest

plugins {
    base
}

abstract class ComputeSha1 : DefaultTask() {

    @get:InputFile
    abstract val inputFile: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun compute() {
        val hash = MessageDigest.getInstance("SHA-1")
            .digest(inputFile.get().asFile.readBytes())
            .joinToString("") { "%02x".format(it) }
        outputFile.get().asFile.writeText(hash)
        logger.lifecycle("Resource pack SHA-1: $hash")
    }
}

val packResourcePack by tasks.registering(Zip::class) {
    group = "build"
    description = "Assembles the Minecraft resource pack zip."

    archiveBaseName = rootProject.name
    archiveVersion = providers.gradleProperty("projectVersion")
    destinationDirectory = layout.buildDirectory.dir("libs")

    from("src")
    exclude("**/.DS_Store", "**/Thumbs.db", "**/*.bak")
}

val computePackHash by tasks.registering(ComputeSha1::class) {
    group = "build"
    description = "Computes the SHA-1 hash of the resource pack zip."

    inputFile.set(packResourcePack.flatMap { it.archiveFile })
    outputFile.set(
        layout.buildDirectory.file(
            providers.provider {
                val name = packResourcePack.get().archiveFileName.get()
                "libs/${name.removeSuffix(".zip")}.sha1"
            }
        )
    )
}

tasks.assemble {
    dependsOn(packResourcePack, computePackHash)
}

val packHashElements by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = true
    description = "Exposes the resource pack SHA-1 hash file to dependents."
}

artifacts {
    add("packHashElements", computePackHash.flatMap { it.outputFile }) {
        builtBy(computePackHash)
    }
}
