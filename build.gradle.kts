plugins {
    `maven-publish`
    kotlin("jvm") version "2.0.0"
}

val GITHUB_REPO_URL = "https://github.com/sharedwonder/ptbridge"
val GITHUB_PKG_URL = "https://maven.pkg.github.com/sharedwonder/maven-repository"

group = "net.sharedwonder.mc"
version = property("version").toString()

repositories {
    mavenLocal()
    mavenCentral()
    maven(GITHUB_PKG_URL)
}

dependencies {
    implementation("com.beust:jcommander:1.82")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation(platform("io.netty:netty-bom:4.1.111.Final"))
    implementation("io.netty:netty-buffer")
    implementation("io.netty:netty-codec")
    implementation("io.netty:netty-common")
    implementation(platform("org.apache.logging.log4j:log4j-bom:2.23.1"))
    implementation("org.apache.logging.log4j:log4j-api")
    compileOnly("org.jetbrains:annotations:24.1.0")
    runtimeOnly("org.apache.logging.log4j:log4j-core")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
    withSourcesJar()
}

kotlin {
    compilerOptions.freeCompilerArgs.add("-Xjvm-default=all")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name = project.name
                description = project.description
                url = GITHUB_REPO_URL

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
                    connection = "scm:git:$GITHUB_REPO_URL.git"
                    developerConnection = "scm:git:$GITHUB_REPO_URL.git"
                    url = GITHUB_REPO_URL
                }
            }
        }
    }

    repositories {
        mavenLocal()
        maven(GITHUB_PKG_URL) {
            name = "GitHubPackages"
            credentials {
                username = findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
                password = findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
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
    manifest.attributes["Main-Class"] = "net.sharedwonder.mc.ptbridge.Main"
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Copy>("copyDependencies") {
    from(configurations.runtimeClasspath)
    into("${layout.buildDirectory.get().asFile}/dependencies/main")
}
