plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id ("org.jetbrains.kotlin.jvm") version "1.7.0"
}

group = "com.boy0000.fixblueprints"
version = "0.1-SNAPSHOT"
description = "Write something here idk.\n"


repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://mvn.lumine.io/repository/maven-public/")
    maven("https://repo.mineinabyss.com/releases")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.19.2-R0.1-SNAPSHOT")
    compileOnly("com.ticxo.modelengine:api:R3.0.0")
    implementation("me.mattstudios.utils:matt-framework:1.4.6")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

val copyJar = project.findProperty("copyJar")
val pluginPath = project.findProperty("plugin_path")
tasks {
    shadowJar {
        archiveFileName.set("FixBlueprints.jar")
    }

    if(copyJar != "false" && pluginPath != null) {
        register<Copy>("copyJar") {
            from(findByName("reobfJar") ?: findByName("shadowJar") ?: findByName("jar"))
            into(pluginPath)
            doLast {
                println("Copied to plugin directory $pluginPath")
            }
        }

        build {
            dependsOn("copyJar")
        }
    } else {
        build {
            dependsOn("shadowJar")
        }
    }
}
