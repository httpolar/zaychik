val mainPath = "zaychik.MainKt"

val koinVersion = "3.4.0"
val exposedVersion = "0.41.1"

fun kotlinx(name: String, version: String) = "org.jetbrains.kotlinx:kotlinx-$name:$version"

plugins {
    kotlin("jvm") version "1.8.21"
    kotlin("plugin.serialization") version "1.8.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

application {
    mainClass.set(mainPath)
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8")
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("io.github.oshai", "kotlin-logging-jvm", "4.0.0-beta-28")
    implementation("ch.qos.logback", "logback-classic", "1.4.7")
    implementation(kotlinx("cli-jvm", "0.3.5"))
    implementation(kotlinx("datetime", "0.4.0"))
    implementation(kotlinx("coroutines-core", "1.7.1"))
    implementation(kotlinx("serialization-json", "1.5.1"))
    implementation("dev.kord", "kord-core", "0.9.0")
    implementation("com.zaxxer", "HikariCP", "5.0.1")
    implementation("org.postgresql", "postgresql", "42.6.0")
    implementation("org.jetbrains.exposed", "exposed-core", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-dao", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-jdbc", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-kotlin-datetime", exposedVersion)
    implementation("io.insert-koin", "koin-core", koinVersion)
//    implementation("io.insert-koin", "koin-core-coroutines", koinVersion)
}

