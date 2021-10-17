plugins {
    val kotlinVersion = "1.5.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
}

kotlin.sourceSets.all { languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn") }

group = "org.example"
version = "0.1.0"

repositories {
    mavenLocal()
    jcenter()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("net.mamoe:mirai-core:2.7.1")
    implementation("net.mamoe:mirai-console:2.7.1")
    implementation("net.mamoe:mirai-console-terminal:2.7.1")
    implementation("com.github.nintha:webp-imageio-core:v0.1.3")
}