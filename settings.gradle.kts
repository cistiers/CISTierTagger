rootProject.name = "cistiertagger"

pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.kikugie.dev/snapshots")
        gradlePluginPortal()
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.10-beta.2"
    id("dev.kikugie.loom-back-compat") version "0.4.3"
}

stonecutter.create(rootProject) {
    versions(
        "1.20.1",
        "1.20.2",
        "1.20.3",
        "1.20.5",
        "1.21",
        "1.21.2",
        "1.21.4",
        "1.21.5",
        "1.21.6",
        "1.21.9",
        "1.21.11",
        "26.1",
        "26.2"
    )
    vcsVersion.set("26.2")
}
