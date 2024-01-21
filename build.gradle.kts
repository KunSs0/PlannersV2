plugins {
    `java-library`
    `maven-publish`
    id("org.jetbrains.kotlin.jvm") version "1.9.21"
    id("io.izzel.taboolib") version "1.56"
}

taboolib {
    install("common")
    install("common-5")
    install("module-kether")
    install("module-nms")
    install("module-nms-util")
    install("module-ai")
    install("module-lang")
    install("module-ui")
    install("module-database")
    install("module-navigation")
    install("module-chat")
    install("module-configuration")
    install("platform-bukkit")
    install("expansion-command-helper")
    install("expansion-player-fake-op")
    classifier = null
    version = "6.0.12-69"
    description {
        dependencies {
            name("PlaceholderAPI").optional(true)
            name("MythicMobs").optional(true)
        }
    }
}
tasks.withType<Jar> {
    destinationDir = file("F:/Server/Spigot 1.12.2 - Minigame/plugins")
//    destinationDir = file("F:/Server/paper 1.19.4/plugins")
}
repositories {
    // 依赖使用阿里云 maven 源
    maven {
        setUrl("https://maven.aliyun.com/repository/public/")
    }
    maven {
        setUrl("https://maven.aliyun.com/repository/spring/")
    }
    mavenLocal()
    mavenCentral()
}
dependencies {
    implementation("ink.ptms:nms-all:1.0.0")
    implementation("ink.ptms.core:v11902:11902-minimize:mapped")
    implementation("ink.ptms.core:v11902:11902-minimize:universal")
    implementation("ink.ptms:nms-all:1.0.0")

    implementation("com.google.code.gson:gson:2.8.9")
    implementation("com.google.guava:guava:30.0-android")

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
