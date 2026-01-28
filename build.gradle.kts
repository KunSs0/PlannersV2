import io.izzel.taboolib.gradle.*

plugins {
    java
    id("io.izzel.taboolib") version "2.0.27"
    id("org.jetbrains.kotlin.jvm") version "1.8.22"
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
        }
    }

    version { taboolib = "6.2.4-7c873bc" }

    relocate("ink.ptms.um", "com.gitee.module.um")
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.tabooproject.org/repository/releases")
}
tasks.withType<Jar> {
//    destinationDir = file("$projectDir/build-jar")
//    destinationDirectory = file("F:\\minecraft\\server\\paper-1.12.2\\plugins")
    destinationDirectory = file("F:\\minecraft\\server\\paper-1.20.1\\plugins")
//    destinationDir = file("F:/Server/paper 1.19.4/plugins")
}
dependencies {

    compileOnly("ink.ptms:nms-all:1.0.0")
    compileOnly("ink.ptms.core:v12004:12004:mapped")
    compileOnly("ink.ptms.core:v12004:12004:universal")

    compileOnly("com.google.code.gson:gson:2.8.9")
    // https://mvnrepository.com/artifact/com.google.guava/guava
    implementation("com.google.guava:guava:33.0.0-jre")

    // Fluxon 脚本引擎
    taboo("org.tabooproject.fluxon:core:1.6.1")

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
    compileOnly("com.mojang:datafixerupper:4.0.26")
    compileOnly(kotlin("stdlib"))
    compileOnly(fileTree("libs"))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
