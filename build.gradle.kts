plugins {
    java
    application
    kotlin("jvm") version "2.1.10"
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(platform("io.netty:netty-bom:4.2.1.Final"))
    implementation("io.netty:netty-buffer")
    implementation("io.netty:netty-codec")
    implementation("io.netty:netty-common")

    implementation(platform("org.apache.logging.log4j:log4j-bom:2.24.1"))
    implementation("org.apache.logging.log4j:log4j-api")
    implementation("org.apache.logging.log4j:log4j-core")

    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.beust:jcommander:1.82")
    implementation("org.jspecify:jspecify:1.0.0")
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

tasks.withType(JavaCompile::class).configureEach {
    options.encoding = "UTF-8"
}

tasks.processResources {
    filesMatching("version.json") {
        expand("version" to version)
    }
}

tasks.register("copyDependencies", Copy::class) {
    from(configurations.runtimeClasspath)
    into("${layout.buildDirectory.get()}/dependencies/main")
}
