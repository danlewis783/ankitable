/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Java application project to get you started.
 * For more details on building Java & JVM projects, please refer to https://docs.gradle.org/8.5/userguide/building_java_projects.html in the Gradle documentation.
 */

plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.junit.jupiter)
	
	testImplementation(libs.assertj)
	testImplementation(libs.assertj.jsoup)
	testImplementation(libs.jsoup)

    testRuntimeOnly(libs.junit.platform.launcher)
    implementation(libs.commons.csv)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

application {
    mainClass.set("ankitable.App")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    outputs.upToDateWhen {false}
    systemProperty("test.resources", "$projectDir/src/test/resources")
}

tasks.named("processTestResources") {
    enabled = false
}
