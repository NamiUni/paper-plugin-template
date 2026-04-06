plugins {
    id("paper-plugin-template.base")
    id("paper-plugin-template.maven-publish")
}

dependencies {
    compileOnlyApi(libs.jspecify)
    compileOnlyApi(libs.adventure.api)
}
