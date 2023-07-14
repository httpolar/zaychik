import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments

val mainPath = "zaychik.MainKt"

val koinVersion = "3.4.2"
val exposedVersion = "0.41.1"

fun kotlinx(name: String, version: String) = "org.jetbrains.kotlinx:kotlinx-$name:$version"

plugins {
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.github.ben-manes.versions") version "0.47.0"
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
    K2JVMCompilerArguments().useK2 = true
    jvmToolchain(17)
}

dependencies {
    implementation("io.github.oshai", "kotlin-logging-jvm", "5.0.0-beta-04")
    implementation("ch.qos.logback", "logback-classic", "1.4.8")
    implementation(kotlinx("cli-jvm", "0.3.5"))
    implementation(kotlinx("datetime", "0.4.0"))
    implementation(kotlinx("coroutines-core", "1.7.2"))
    implementation(kotlinx("serialization-json", "1.5.1"))
    implementation(kotlinx("collections-immutable", "0.3.5"))
    implementation("dev.kord", "kord-core", "0.10.0")
    implementation("com.zaxxer", "HikariCP", "5.0.1")
    implementation("org.postgresql", "postgresql", "42.6.0")
    implementation("org.jetbrains.exposed", "exposed-core", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-dao", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-jdbc", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-kotlin-datetime", exposedVersion)
    implementation("io.insert-koin", "koin-core", koinVersion)
//    implementation("io.insert-koin", "koin-core-coroutines", koinVersion)
}

