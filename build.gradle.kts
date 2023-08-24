plugins {
    java
    `maven-publish`
}

group = "cc.tweaked"
version = "1.8.0"

java {
    withJavadocJar()
    withSourcesJar()

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
    maven("https://squiddev.cc/maven") {
        content {
            includeGroup("cc.tweaked")
            includeModule("org.squiddev", "Cobalt")
        }
    }
}

dependencies {
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    implementation("com.google.auto.service:auto-service:1.0.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.0")
    testImplementation("cc.tweaked:cc-tweaked-1.19.3-core:1.102.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }

    repositories {
        maven("https://squiddev.cc/maven") {
            name = "SquidDev"
            credentials(PasswordCredentials::class)
        }
    }
}
