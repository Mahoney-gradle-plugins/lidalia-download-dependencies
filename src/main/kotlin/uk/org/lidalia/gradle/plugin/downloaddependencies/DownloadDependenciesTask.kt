package uk.org.lidalia.gradle.plugin.downloaddependencies

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class DownloadDependenciesTask : DefaultTask() {

  @TaskAction
  fun downloadDependencies() {
    project.configurations.resolveAll()
    project.buildscript.configurations.resolveAll()
    logger.info("Downloaded all dependencies")
  }
}
