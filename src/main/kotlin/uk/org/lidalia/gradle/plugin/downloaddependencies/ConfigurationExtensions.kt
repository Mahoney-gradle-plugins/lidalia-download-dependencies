package uk.org.lidalia.gradle.plugin.downloaddependencies

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.internal.deprecation.DeprecatableConfiguration

fun Configuration.isDeprecated(): Boolean =
  this is DeprecatableConfiguration && resolutionAlternatives != null

fun ConfigurationContainer.resolveAll() = this
  .filter { it.isCanBeResolved && !it.isDeprecated() }
  .forEach { it.resolve() }
