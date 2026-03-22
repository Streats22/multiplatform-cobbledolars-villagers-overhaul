plugins {
    id("java-library")
    id("dev.architectury.loom") version "1.11-SNAPSHOT"
}

group = rootProject.extra["maven_group"]!!
version = rootProject.version

loom {
    silentMojangMappingsLicense()
}

repositories {
    maven("https://maven.fabricmc.net/")
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
