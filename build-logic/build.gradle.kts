plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(libs.indra.licenser.spotless)
    implementation(libs.shadow)
    implementation(libs.gremlin.gradle)
    implementation(libs.maven.publish)

    // https://github.com/gradle/gradle/issues/15383#issuecomment-779893192
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
