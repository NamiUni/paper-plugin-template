import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

plugins {
    id("java-library")
    id("com.vanniktech.maven.publish")
    id("paper-plugin-template.metadata")
}

val metadata = extensions.getByType<ProjectMetadata>()

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    configure(
        JavaLibrary(
            javadocJar = JavadocJar.Javadoc(),
            sourcesJar = SourcesJar.Sources(),
        )
    )

    pom {
        name.set(metadata.projectName)
        description.set(metadata.projectDescription)
        url.set(metadata.projectWebsite)
        licenses {
            license {
                name.set("The GNU General Public License, Version 3")
                url.set("https://www.gnu.org/licenses/gpl-3.0.html")
            }
        }
        developers {
            developer {
                id.set(metadata.authorId)
                name.set(metadata.authorName)
                url.set(metadata.authorWebsite)
            }
        }
        scm {
            url.set(metadata.projectWebsite)
            connection.set("scm:git:git://github.com/${metadata.authorId.get()}/${rootProject.name}.git")
            developerConnection.set("scm:git:ssh://git@github.com/${metadata.authorId.get()}/${rootProject.name}.git")
        }
    }
}
