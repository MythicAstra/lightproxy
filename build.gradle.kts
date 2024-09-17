plugins {
    java
    `maven-publish`
    signing
    kotlin("jvm") version "2.0.20"
}

val repoUrl = "https://github.com/sharedwonder/lightproxy"
val pkgUrl = "https://maven.pkg.github.com/sharedwonder/maven-repository"

group = "net.sharedwonder"
version = property("version").toString()

repositories {
    mavenLocal()
    mavenCentral()
    maven(pkgUrl)
}

dependencies {
    implementation(platform("io.netty:netty-bom:4.1.112.Final"))
    implementation("io.netty:netty-buffer")
    implementation("io.netty:netty-codec")
    implementation("io.netty:netty-common")

    implementation(platform("org.apache.logging.log4j:log4j-bom:2.24.0"))
    implementation("org.apache.logging.log4j:log4j-api")
    runtimeOnly("org.apache.logging.log4j:log4j-core")

    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.beust:jcommander:1.82")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
    withSourcesJar()
}

kotlin {
    compilerOptions.freeCompilerArgs.add("-Xjvm-default=all")
}

publishing {
    repositories {
        mavenLocal()
        maven(pkgUrl) {
            name = "GitHubPackages"
            credentials {
                username = findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
                password = findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }

    publications {
        register<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name = project.name
                description = project.description
                url = repoUrl

                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0"
                    }
                }

                developers {
                    developer {
                        id = "sharedwonder"
                        name = "Liu Baihao"
                        email = "liubaihaohello@outlook.com"
                    }
                }

                scm {
                    connection = "scm:git:$repoUrl.git"
                    developerConnection = "scm:git:$repoUrl.git"
                    url = repoUrl
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["maven"])
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.processResources {
    filesMatching("version.json") {
        expand("version" to version)
    }
}

tasks.jar {
    manifest.attributes["Main-Class"] = "net.sharedwonder.lightproxy.Main"
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Copy>("copyDependencies") {
    from(configurations.runtimeClasspath)
    into("${layout.buildDirectory.get().asFile}/dependencies/main")
}
