plugins {
    `java-library`
    id("dev.kikugie.stonecutter")
    id("dev.kikugie.loom-back-compat")
}

val target = stonecutter.properties
val minecraftVersion = stonecutter.current.version
val loaderVersion = target["deps.fabric_loader"]
val fabricApiVersion = target["deps.fabric_api"]
val modMenuVersion = target["deps.modmenu"]
val lombokVersion = target["deps.lombok"]
val annotationsVersion = target["deps.annotations"]
val junitVersion = target["deps.junit"]
val javaVersion = target.getAs<Int>("mod.java_version")

group = target["mod.group"]
version = "${providers.gradleProperty("mod.version").getOrElse(target["mod.version"])}+$minecraftVersion"

base {
    archivesName.set(target["mod.archive"])
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaVersion)
}

repositories {
    maven("https://api.modrinth.com/maven")
    maven("https://maven.parchmentmc.org")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    if (!project.loomx.isUnobfuscated) {
        val parchmentMappings = target["deps.parchment"]
        mappings(loom.layered {
            officialMojangMappings()
            parchment(parchmentMappings)
        })
    }
    modApi("net.fabricmc:fabric-loader:$loaderVersion")
    modApi("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    modApi("maven.modrinth:modmenu:$modMenuVersion")

    val lombok = "org.projectlombok:lombok:$lombokVersion"
    compileOnly(lombok)
    annotationProcessor(lombok)
    compileOnly("org.jetbrains:annotations:$annotationsVersion")
    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    val properties = mapOf(
        "version" to project.version.toString(),
        "minecraft_version" to minecraftVersion,
        "minecraft_range" to target["mod.minecraft_range"],
        "loader_version" to loaderVersion,
        "archives_base_name" to base.archivesName.get(),
        "mixin_compatibility" to "JAVA_$javaVersion"
    )
    inputs.properties(properties)
    filteringCharset = "UTF-8"
    filesMatching(listOf("fabric.mod.json", "cistiertagger.mixins.json", "cistiertagger.modmenu.mixins.json")) {
        expand(properties)
    }
}

tasks.jar {
    from(rootProject.file("license")) {
        rename { "${it}_${base.archivesName.get()}" }
    }
}

loom.runs.configureEach {
    runDirectory.set(rootProject.layout.projectDirectory.dir("runs/$minecraftVersion"))
}
