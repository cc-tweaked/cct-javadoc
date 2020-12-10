plugins {
    java
    `maven-publish`
    maven
}

group = "cc.tweaked"
version = "1.3.0"

java {
    withJavadocJar()
    withSourcesJar()
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_9
}

repositories {
    mavenCentral()
    maven("https://squiddev.cc/maven")
}

val deployerJars by configurations.creating

dependencies {
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    implementation("com.google.auto.service:auto-service:1.0-rc7")
    implementation("org.squiddev:cc-tweaked-1.15.2:1.94.0")
    deployerJars("org.apache.maven.wagon:wagon-ssh:3.3.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

// While the maven plugin/uploadArchives has been deprecated for a long time, I've not found a good way to handle scp
// repositories (which is what mavenUploadUrl is).
tasks.named<Upload>("uploadArchives") {
    if (project.hasProperty("mavenUploadUrl")) {
        repositories.withGroovyBuilder {
            "mavenDeployer" {
                setProperty("configuration", deployerJars)

                "repository"("url" to project.property("mavenUploadUrl")) {
                    "authentication"(
                            "userName" to project.property("mavenUploadUser"),
                            "privateKey" to project.property("mavenUploadKey")
                    )
                }
            }
        }
    }
}
tasks.getByPath("publish").dependsOn("uploadArchives")

tasks.named<Test>("test") {
    useJUnitPlatform()
    testLogging {
        events("skipped", "failed")
    }
}
