import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments

val mainPath = "zaychik.MainKt"

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.serialization)
    alias(libs.plugins.shadow)
    alias(libs.plugins.dep.versions)
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
    implementation(libs.cli)
    implementation(libs.koin)
    implementation(libs.coroutines)
    implementation(libs.datetime)
    implementation(libs.serialization.json)
    implementation(libs.immutable.collections)
    implementation(libs.logger.backend)
    implementation(libs.logger)
    implementation(libs.kord)
    implementation(libs.hikari)
    implementation(libs.postgresql)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.datetime)
}

