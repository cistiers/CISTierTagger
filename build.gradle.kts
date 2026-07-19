plugins {
    java
    alias(libs.plugins.loom)
}

group = providers.gradleProperty("maven_group").get()
version = "${providers.gradleProperty("mod_version").get()}+${libs.versions.minecraft.get()}"

base {
    archivesName.set(providers.gradleProperty("archives_base_name").get())
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

repositories {
    maven("https://maven.terraformersmc.com/releases/")
}

dependencies {
    minecraft(libs.minecraft)

    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)
    implementation(libs.modmenu)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    compileOnly(libs.annotations)
}

tasks.processResources {
    val properties = mapOf(
        "version"            to project.version.toString(),
        "minecraft_version"  to libs.versions.minecraft.get(),
        "loader_version"     to libs.versions.loader.get(),
        "archives_base_name" to providers.gradleProperty("archives_base_name").get()
    )
    inputs.properties(properties)
    filteringCharset = "UTF-8"
    filesMatching("fabric.mod.json") {
        expand(properties)
    }
}

tasks.jar {
    from(rootProject.file("license")) {
        rename { "${it}_${base.archivesName.get()}" }
    }
}
