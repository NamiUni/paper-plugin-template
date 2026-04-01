plugins {
    id("java-library")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-Xshare:off")
}

dependencies {
    testImplementation(libs.bundles.testing)
    testRuntimeOnly(libs.junit.platform)
    testImplementation(libs.adventure.api)
}
