import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.dokka") version "1.6.10"
    `maven-publish`
}

group = "com.github.Vincentvibe3"
version = "1.2.14"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("io.ktor:ktor-client-core:1.6.8")
    implementation("io.ktor:ktor-client-cio:1.6.8")
    implementation("org.json:json:20220320")
}

tasks.test {
    useJUnitPlatform()
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
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
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}