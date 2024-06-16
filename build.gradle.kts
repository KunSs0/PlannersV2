import io.izzel.taboolib.gradle.*

plugins {
    java
    id("io.izzel.taboolib") version "2.0.12"
    id("org.jetbrains.kotlin.jvm") version "1.8.22"
}

taboolib {
    env {
        // 安装模块
        install(
            KETHER,
            LANG,
            CHAT,
            CONFIGURATION,
            NMS,
            NMS_UTIL,
            UI,
            EXPANSION_COMMAND_HELPER,
            UNIVERSAL,
            BUKKIT,
            BUKKIT_UTIL,
            BUKKIT_HOOK,
            NAVIGATION,
            BUKKIT_XSERIES,
            DATABASE,
            AI,
            EXPANSION_PLAYER_FAKE_OP,
            METRICS
        )
        install("module-nms-util-unstable")
    }
    version {
        taboolib = "6.1.2-beta7"
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}
tasks.withType<Jar> {
//    destinationDir = file("$projectDir/build-jar")
    destinationDirectory = file("F:/Server/Spigot 1.12.2 - Minigame/plugins")
//    destinationDir = file("F:/Server/paper 1.19.4/plugins")
}
repositories {
    mavenCentral()
}
dependencies {
    compileOnly("ink.ptms.core:v11902:11902-minimize:mapped")
    compileOnly("ink.ptms.core:v11902:11902-minimize:universal")

    compileOnly("com.google.code.gson:gson:2.8.9")
    // https://mvnrepository.com/artifact/com.google.guava/guava
    implementation("com.google.guava:guava:33.0.0-jre")

    // https://mvnrepository.com/artifact/org.ejml/ejml-all
    compileOnly("org.ejml:ejml-core:0.41")
    compileOnly("org.ejml:ejml-simple:0.41")
    compileOnly("org.ejml:ejml-fdense:0.41")
    compileOnly("public:ModelEngine:2.5.1")

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
