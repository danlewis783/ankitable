plugins {
    application
}

repositories {
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
    systemProperty("test.resources", "$projectDir/src/test/resources")
}

tasks.named<JavaCompile>("compileJava") {
    options.encoding = "UTF-8"
}