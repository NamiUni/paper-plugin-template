val config = extensions.create<ProjectMetadata>("projectMetadata")

config.projectName.convention(providers.gradleProperty("projectName"))
config.projectDescription.convention(providers.gradleProperty("projectDescription"))
config.projectVersion.convention(providers.gradleProperty("projectVersion"))
config.projectGroup.convention(providers.gradleProperty("projectGroup"))
config.projectWebsite.convention(providers.gradleProperty("projectWebsite"))
config.projectContributors.convention(providers.gradleProperty("projectContributors"))

config.authorId.convention(providers.gradleProperty("authorId"))
config.authorName.convention(providers.gradleProperty("authorName"))
config.authorWebsite.convention(providers.gradleProperty("authorWebsite"))
