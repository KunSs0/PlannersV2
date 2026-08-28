import io.izzel.taboolib.gradle.*

plugins {
    java
    id("io.izzel.taboolib") version "2.0.27"
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
}

taboolib {
    env {
        install(Basic)
        install(Bukkit)
        install(BukkitUI)
        install(BukkitUtil)
        install(CommandHelper)
        install(BukkitHook)
        install(XSeries)
        install(MinecraftEffect)
        install(Metrics)
        install(BukkitNavigation)
        install(Database)
        install(DatabasePlayer)
        install(BukkitNMSEntityAI)
        install(BukkitFakeOp)
    }

    description {
        dependencies {
            name("MythicMobs").optional(true)
            name("PlaceholderAPI").optional(true)
            name("NovaLang")
            name("Vault").optional(true)
        }
    }

    version { taboolib = "6.3.0-9ccc4c3" }

    relocate("ink.ptms.um", "com.gitee.module.um")
}

repositories {
    mavenCentral()
    maven("https://repo.tabooproject.org/repository/releases")
    maven("https://repo.papermc.io/repository/maven-public/")
}
dependencies {

    compileOnly("ink.ptms:nms-all:1.0.0")
    compileOnly("ink.ptms.core:v12004:12004:mapped")
    compileOnly("ink.ptms.core:v12004:12004:universal")

    compileOnly("com.google.code.gson:gson:2.8.9")
    // https://mvnrepository.com/artifact/com.google.guava/guava
    implementation("com.google.guava:guava:33.0.0-jre")

    // https://mvnrepository.com/artifact/org.ejml/ejml-all
    compileOnly("org.ejml:ejml-core:0.41")
    compileOnly("org.ejml:ejml-simple:0.41")
    compileOnly("org.ejml:ejml-fdense:0.41")
    compileOnly("org.ejml:ejml-ddense:0.41")
    compileOnly("public:ModelEngine:2.5.1")
    compileOnly("public:WorldGuard:7.0.7")
    compileOnly("com.sk89q.worldedit:WorldEdit:7")
    taboo("ink.ptms:um:1.0.0-beta-18")

    testCompileOnly("org.ejml:ejml-core:0.41")
    testCompileOnly("org.ejml:ejml-simple:0.41")
    testCompileOnly("org.ejml:ejml-fdense:0.41")
    testRuntimeOnly("org.ejml:ejml-simple:0.41")
    compileOnly("com.mojang:datafixerupper:4.0.26")

    compileOnly(kotlin("stdlib"))
    compileOnly(fileTree("libs"))
    testImplementation(files("libs/nova-runtime-workspace-0.2.0.jar"))
    testImplementation(files("libs/nova-runtime-all-0.2.0.jar"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.91.1") {
        exclude(group = "io.papermc.paper", module = "paper-api")
        exclude(group = "org.junit.jupiter")
        exclude(group = "org.junit.platform")
        exclude(group = "org.junit")
    }
    testImplementation("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    testImplementation("net.kyori:adventure-api:4.24.0")
    testImplementation("net.kyori:adventure-key:4.24.0")
    testImplementation("net.kyori:adventure-text-minimessage:4.24.0")
    testImplementation("net.kyori:examination-api:1.3.0")
    testImplementation("net.md-5:bungeecord-chat:1.21-R0.2-deprecated")
    testImplementation("com.electronwill.night-config:core:3.6.7")
    testImplementation("com.electronwill.night-config:hocon:3.6.7")
    testImplementation("com.electronwill.night-config:json:3.6.7")
    testImplementation("com.electronwill.night-config:toml:3.6.7")
    testImplementation("org.yaml:snakeyaml:2.6")
    testImplementation("it.unimi.dsi:fastutil:8.5.15")
    testImplementation("com.google.code.gson:gson:2.11.0")
    testImplementation("com.mojang:brigadier:1.3.10")
    testRuntimeOnly(files("libs/[经济]Vault[前置插件].jar"))
    testImplementation("org.xerial:sqlite-jdbc:3.49.1.0")
    val extraTestLibraries = providers.gradleProperty("planners.test.extraLibs")
    if (extraTestLibraries.isPresent) {
        val extraFiles = fileTree(mapOf("dir" to extraTestLibraries.get(), "include" to listOf("**/*.jar")))
        testImplementation(extraFiles)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

configurations.named("testCompileClasspath") {
    attributes {
        attribute(org.gradle.api.attributes.java.TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
    }
}

configurations.named("testRuntimeClasspath") {
    attributes {
        attribute(org.gradle.api.attributes.java.TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
    }
}

tasks.test {
    useJUnitPlatform()
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    })
    testLogging {
        showStandardStreams = true
    }
}

val apiJar by tasks.registering(Jar::class) {
    description = "Build the independent Planners API jar for compile-time plugin integrations"
    group = "build"
    dependsOn(tasks.named("classes"))
    archiveClassifier.set("api")
    from(sourceSets.main.get().output.classesDirs)
}

tasks.named("taboolibBuildApi") {
    finalizedBy(apiJar)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
