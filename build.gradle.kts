import java.net.URI

plugins {
    id("java")
    id("maven-publish")
    id("com.gradleup.shadow") version "8.3.0"
}

group = "net.glasslauncher"
version = "1.0+thisisawful"

repositories {
    maven {
        url = URI("https://maven.fabricmc.net")
    }
    mavenCentral()
}

dependencies {
    implementation("net.fabricmc:mapping-io:0.6.1")
    implementation("org.jetbrains:annotations:24.0.0")
}

tasks.withType<Jar> {
    manifest.attributes["Main-Class"] = "net.glasslauncher.intermediaryruiner.Main"
}

// configure the maven publication
publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            project.shadow.component(this)
        }
    }

    repositories {
        mavenLocal()
        if (project.hasProperty("glass_maven_username")) {
            maven {
                url = URI("https://maven.glass-launcher.net/releases")
                credentials {
                    username = "${project.properties["glass_maven_username"]}"
                    password = "${project.properties["glass_maven_password"]}"
                }
            }
        }
    }
}