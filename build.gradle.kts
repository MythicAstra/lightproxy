plugins {
    java
    kotlin("jvm") version "2.0.20"
    application
}

group = "net.sharedwonder"
version = property("version") as String

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(platform("io.netty:netty-bom:4.1.112.Final"))
    implementation("io.netty:netty-buffer")
    implementation("io.netty:netty-codec")
    implementation("io.netty:netty-common")

    implementation(platform("org.apache.logging.log4j:log4j-bom:2.24.1"))
    implementation("org.apache.logging.log4j:log4j-api")
    runtimeOnly("org.apache.logging.log4j:log4j-core")

    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.beust:jcommander:1.82")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
    withSourcesJar()
}

kotlin {
    compilerOptions.freeCompilerArgs.add("-Xjvm-default=all")
}

application {
    mainClass = "net.sharedwonder.lightproxy.Main"
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.processResources {
    filesMatching("version.json") {
        expand("version" to version)
    }
}

tasks.test {
    useJUnitPlatform()
}

task("copyDependencies", Copy::class) {
    from(configurations.runtimeClasspath)
    into("${layout.buildDirectory.get().asFile}/dependencies/main")
}
