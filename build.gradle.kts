plugins {
    java
    `maven-publish`
    maven
}

group = "cc.tweaked"
version = "1.2.0"

java {
    withJavadocJar()
    withSourcesJar()
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_9
}

repositories {
    mavenCentral()
}

val deployerJars by configurations.creating

dependencies {
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    implementation("com.google.auto.service:auto-service:1.0-rc7")
    deployerJars("org.apache.maven.wagon:wagon-ssh:3.3.1")
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
