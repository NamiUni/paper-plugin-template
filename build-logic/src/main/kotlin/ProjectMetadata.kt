import org.gradle.api.provider.Property

interface ProjectMetadata {
    val projectName: Property<String>
    val projectDescription: Property<String>
    val projectVersion: Property<String>
    val projectGroup: Property<String>
    val projectContributors: Property<String>
    val projectWebsite: Property<String>
    val authorId: Property<String>
    val authorName: Property<String>
    val authorWebsite: Property<String>
}
