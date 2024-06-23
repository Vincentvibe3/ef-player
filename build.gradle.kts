import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.0"
    id("org.jetbrains.dokka") version "1.6.10"
    kotlin("plugin.serialization") version "1.8.0"
    `maven-publish`
}

group = "com.github.Vincentvibe3"
version = "1.4.6"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0-RC")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    api("org.slf4j:slf4j-api:2.0.0")
}

tasks.test {
    useJUnitPlatform()
}


kotlin {
    jvmToolchain(17)
}

sourceSets {
    main {
        java.srcDir("src/main/kotlin")
    }
}

tasks.dokkaHtml.configure {
    outputDirectory.set(project.projectDir.resolve("docs"))
    dokkaSourceSets {
        configureEach {
            jdkVersion.set(17)
        }
    }
}

tasks.dokkaJavadoc.configure {
    outputDirectory.set(buildDir.resolve("dokka/javadoc"))
    dokkaSourceSets {
        configureEach {
            jdkVersion.set(17)
        }
    }
}

tasks.register<Jar>("javadocJar")
tasks.register<Jar>("sourcesJar")

tasks {
    named<Jar>("javadocJar"){
        archiveClassifier.set("javadoc")
        dependsOn("dokkaJavadoc")
        from(buildDir.resolve("dokka/javadoc"))
    }

    named<Jar>("sourcesJar"){
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Vincentvibe3/ef-player")
            credentials {
                username = System.getenv("USERNAME")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
    publications {
        register<MavenPublication>("gpr") {
            from(components["java"])
            artifact("${buildDir.resolve("libs")}/efplayer-${project.version}-sources.jar"){
                classifier = "sources"
            }
            artifact("${buildDir.resolve("libs")}/efplayer-${project.version}-javadoc.jar"){
                classifier = "javadoc"
            }
        }

    }
}

tasks.withType<PublishToMavenRepository>{
    dependsOn("sourcesJar")
    dependsOn("javadocJar")
}

tasks.withType<PublishToMavenLocal>{
    dependsOn("sourcesJar")
    dependsOn("javadocJar")
}