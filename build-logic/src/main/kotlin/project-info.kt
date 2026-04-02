import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

interface ProjectMetadata {
    val name: Property<String>
    val version: Property<String>
    val group: Property<String>
    val author: Property<String>
    val contributors: ListProperty<String>
    val website: Property<String>
}
