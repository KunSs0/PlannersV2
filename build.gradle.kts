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
    install("module-lang")
    install("module-chat")
    install("module-configuration")
    install("platform-bukkit")
    classifier = null
    version = "6.0.12-35"
    description {
        dependencies {
            name("PlaceholderAPI")
            name("MythicMobs")
        }
    }
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
    compileOnly("ink.ptms:nms-all:1.0.0")
    compileOnly("ink.ptms.core:v11902:11902-minimize:mapped")
    compileOnly("ink.ptms.core:v11902:11902-minimize:universal")

    implementation("com.google.code.gson:gson:2.8.9")

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
