plugins {
    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    compileOnly(libs.jetbrains.annotations)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)

    testImplementation(libs.assertj)
    testImplementation(libs.assertj.jsoup)
    testImplementation(libs.jsoup)

    testRuntimeOnly(libs.junit.platform.launcher)

    implementation(libs.commons.csv)
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

application {
    // Define the main class for the application.
    mainClass.set("ankitable.App")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    systemProperty("test.resources", "$projectDir/src/test/resources")
}

tasks.named<JavaCompile>("compileJava") {
    options.encoding = "UTF-8"
}