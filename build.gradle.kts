import java.net.URI

group = "uk.org.lidalia.gradle.plugin"

plugins {
  `java-gradle-plugin`
  `kotlin-dsl`
  `maven-publish`
  id("uk.org.lidalia.downloaddependencies") version "0.4.0"
  id("com.autonomousapps.dependency-analysis") version "1.18.0"
  id("org.jmailen.kotlinter") version "3.13.0"
  id("com.github.ben-manes.versions") version "0.44.0"
}

repositories {
  // Use Maven Central for resolving dependencies.
  mavenCentral()
}

dependencies {
  testImplementation(libs.bundles.kotest)
}

gradlePlugin {
  // Define the plugin
  @Suppress("UNUSED_VARIABLE")
  val downloadDependencies by plugins.creating {
    id = "uk.org.lidalia.downloaddependencies"
    version = "0.4.0"
    implementationClass =
      "uk.org.lidalia.gradle.plugin.downloaddependencies.LidaliaDownloadDependenciesPlugin"
  }
}

publishing {
  repositories {
    maven {
      name = "lidalia-public"
      url = URI("s3://lidalia-maven-public-repo/releases/")
      credentials(AwsCredentials::class.java) {
        accessKey = System.getenv("AWS_ACCESS_KEY_ID")
        secretKey = System.getenv("AWS_SECRET_ACCESS_KEY")
        sessionToken = System.getenv("AWS_SESSION_TOKEN")
      }
    }
  }
}

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest") {
}

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])

// Add a task to run the functional tests
val functionalTest by tasks.registering(Test::class) {
  testClassesDirs = functionalTestSourceSet.output.classesDirs
  classpath = functionalTestSourceSet.runtimeClasspath
}

gradlePlugin.testSourceSets(functionalTestSourceSet)

tasks.named<Task>("check") {
  // Run the functional tests as part of `check`
  dependsOn(functionalTest)
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}

tasks {
  check {
    dependsOn("buildHealth")
    dependsOn("installKotlinterPrePushHook")
  }
}

dependencyAnalysis {
  issues {
    // configure for all projects
    all {
      // set behavior for all issue types
      onAny {
        severity("fail")
      }
    }
  }
}
