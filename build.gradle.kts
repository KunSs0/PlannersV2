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
            name("ScriptEngine")
        }
    }

    version { taboolib = "6.3.0-9ccc4c3" }

    relocate("ink.ptms.um", "com.gitee.module.um")
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.tabooproject.org/repository/releases")
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
    compileOnly("com.mojang:datafixerupper:4.0.26")
    compileOnly("org.graalvm.polyglot:polyglot:24.1.1")
    compileOnly("org.graalvm.js:js-language:24.1.1")
    testImplementation("org.graalvm.polyglot:polyglot:24.1.1")
    testImplementation("org.graalvm.js:js-language:24.1.1")

    compileOnly(kotlin("stdlib"))
    compileOnly(fileTree("libs") {
        exclude("ScriptEngine-*.jar")
    })
    compileOnly("com.gitee.scriptengine:scriptengine-common:1.1.0")
    compileOnly("com.gitee.scriptengine:scriptengine-runtime:1.1.0")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

task("runGraalTest", JavaExec::class) {
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("GraalJsThreadTest")
}


tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
