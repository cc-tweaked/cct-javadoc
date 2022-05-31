plugins {
    java
    `maven-publish`
}

group = "cc.tweaked"
version = "1.4.6"

java {
    withJavadocJar()
    withSourcesJar()

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

repositories {
    mavenCentral()
    maven("https://squiddev.cc/maven")
}

val deployerJars by configurations.creating

dependencies {
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    implementation("com.google.auto.service:auto-service:1.0-rc7")
    deployerJars("org.apache.maven.wagon:wagon-ssh:3.3.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.0")
    testImplementation("org.squiddev:cc-tweaked-1.16.5:1.98.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }

    repositories {
        if (project.hasProperty("mavenUser")) {
            maven {
                name = "SquidDev"
                url = uri("https://squiddev.cc/maven")
                credentials {
                    username = project.property("mavenUser") as String
                    password = project.property("mavenPass") as String
                }
            }
        }
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    testLogging {
        events("skipped", "failed")
    }
}
