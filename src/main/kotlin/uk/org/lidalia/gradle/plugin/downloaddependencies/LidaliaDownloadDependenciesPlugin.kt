package uk.org.lidalia.gradle.plugin.downloaddependencies

import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.tasks.TaskReference
import org.gradle.kotlin.dsl.register

@Suppress("unused")
class LidaliaDownloadDependenciesPlugin: Plugin<Project> {

    override fun apply(project: Project) {

        val task = project.tasks.register<DownloadDependenciesTask>(name)

        if (project.isRoot) {
            task.configure {
                dependsOn(*project.includedBuildTasks(":$name"))
            }
        }
    }

    companion object {
        const val name = "downloadDependencies"
    }
}

private val Project.isRoot get() = parent == null

private fun Project.includedBuildTasks(name: String): Array<TaskReference> = gradle.includedBuilds
    .map { it.task(name) }
    .toTypedArray()
