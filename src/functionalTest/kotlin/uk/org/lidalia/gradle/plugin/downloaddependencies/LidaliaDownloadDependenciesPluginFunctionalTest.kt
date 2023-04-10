/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package uk.org.lidalia.gradle.plugin.downloaddependencies

import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.GradleRunner

/**
 * A simple functional test for the 'uk.org.lidalia.downloaddependencies' plugin.
 */
class LidaliaDownloadDependenciesPluginFunctionalTest : StringSpec({

  val tempFolder = tempdir()

  fun getProjectDir() = tempFolder
  fun getBuildFile() = getProjectDir().resolve("build.gradle")
  fun getSettingsFile() = getProjectDir().resolve("settings.gradle")

  "can run task" {
    // Setup the test build
    getSettingsFile().writeText("")
    getBuildFile().writeText(
      """
      plugins {
          id('uk.org.lidalia.downloaddependencies')
      }
      """.trimIndent(),
    )

    // Run the build
    val result = GradleRunner.create()
      .forwardOutput()
      .withPluginClasspath()
      .withArguments("--info", "downloadDependencies")
      .withProjectDir(getProjectDir())
      .build()

    // Verify the result
    result.output shouldContain "Downloaded all dependencies"
  }
})
