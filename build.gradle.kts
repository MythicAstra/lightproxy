buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }

    dependencies {
        classpath("com.google.code.gson:gson:2.10.1")
    }
}

plugins {
    kotlin("jvm")
    `maven-publish`
    id("org.jetbrains.dokka")
}

group = "net.sharedwonder.mc"
version = "0.1.0"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.pkg.github.com/sharedwonder/maven-repository")
}

dependencies {
    implementation(platform("io.netty:netty-bom:4.1.101.Final"))
    implementation(platform("org.apache.logging.log4j:log4j-bom:2.22.1"))
    implementation("com.beust:jcommander:1.82")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("io.netty:netty-buffer")
    implementation("io.netty:netty-codec")
    implementation("io.netty:netty-common")
    implementation("org.apache.logging.log4j:log4j-api")
    implementation("org.jetbrains:annotations:24.0.1")
    runtimeOnly("org.apache.logging.log4j:log4j-core")

    testImplementation(platform("org.junit:junit-bom:5.9.3"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

kotlin {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    compilerOptions.freeCompilerArgs.add("-Xjvm-default=all")
}

publishing {
    publications {
        create<MavenPublication>(name) {
            from(components["java"])
        }
    }

    repositories {
        mavenLocal()
        maven("https://maven.pkg.github.com/sharedwonder/maven-repository") {
            credentials {
                username = findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
                password = findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.processResources {
    dependsOn(generateResources)
    from("${layout.buildDirectory.get().asFile}/generated/resources/main")
}

tasks.jar {
    manifest.attributes["Main-Class"] = "net.sharedwonder.mc.ptbridge.Main"
}

tasks.test {
    useJUnitPlatform()
}

val generateResources by tasks.registering {
    doFirst {
        val file = File("${layout.buildDirectory.get().asFile}/generated/resources/main/META-INF/${project.group}.${project.name}.json")
        file.parentFile.deleteRecursively()
        file.parentFile.mkdirs()
        file.writer().use { com.google.gson.Gson().toJson(mapOf("version" to version), Map::class.java, it) }
    }
}

val copyDependencies by tasks.registering(Copy::class) {
    from(configurations.runtimeClasspath)
    val dest = "${layout.buildDirectory.get().asFile}/dependencies/main"
    delete(dest)
    into(dest)
}
