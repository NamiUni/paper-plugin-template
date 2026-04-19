plugins {
    id("java-library")
    id("com.gradleup.shadow")
    id("xyz.jpenilla.gremlin-gradle")
}

dependencies {
    testImplementation(libs.bundles.testing)
    testRuntimeOnly(libs.junit.platform)
}

configurations.testImplementation {
    extendsFrom(configurations.runtimeDownload.get())
}

tasks.test {
    useJUnitPlatform()
    jvmArgs(
        "-Xshare:off",
        "-javaagent:${configurations.testRuntimeClasspath.get().find { it.name.contains("mockito-core") }}",
        "-XX:+EnableDynamicAgentLoading"
    )
}
