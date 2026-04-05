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

    testImplementation(libs.assertj)
    testImplementation(libs.assertj.jsoup)
    testImplementation(libs.jsoup)

    implementation(libs.commons.csv)
}

testing {
    suites {
        // Configure the built-in test suite
        named<JvmTestSuite>("test") {
            useJUnitJupiter(libs.versions.junit.jupiter.get())
        }
    }
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
    systemProperty("test.resources", "$projectDir/src/test/resources")
}

tasks.named<JavaCompile>("compileJava") {
    options.encoding = "UTF-8"
}