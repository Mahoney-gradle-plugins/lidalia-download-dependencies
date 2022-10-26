plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    @Suppress("DSL_SCOPE_VIOLATION") // Remove once KTIJ-19369 is fixed
    alias(libs.plugins.kotlin)
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation(libs.bundles.kotest)
}

gradlePlugin {
    // Define the plugin
    @Suppress("UNUSED_VARIABLE")
    val downloadDependencies by plugins.creating {
        id = "uk.org.lidalia.gradle.plugin.downloaddependencies"
        implementationClass = "uk.org.lidalia.gradle.plugin.downloaddependencies.LidaliaDownloadDependenciesPlugin"
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
