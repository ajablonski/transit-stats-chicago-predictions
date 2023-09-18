import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0"
    id("com.github.ben-manes.versions") version "0.47.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}


val invoker: Configuration by configurations.creating

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("com.google.cloud.functions:functions-framework-api:1.1.0")

    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(platform("com.google.cloud:libraries-bom:26.23.0"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.2.1")

    testImplementation("com.google.cloud.functions:functions-framework-api:1.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("org.jetbrains.kotlin:kotlin-reflect:1.9.0")

    invoker("com.google.cloud.functions.invoker:java-function-invoker:1.3.0")
}


kotlin {
    jvmToolchain(JavaVersion.VERSION_17.majorVersion.toInt())
}

tasks.withType<DependencyUpdatesTask> {
    val preReleaseVersion = "^.*(rc-?\\d*|m\\d+|-Beta)$".toRegex(RegexOption.IGNORE_CASE)
    rejectVersionIf {
        preReleaseVersion.matches(candidate.version)
    }
    gradleReleaseChannel = "current"
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.register<JavaExec>("runFunction") {
    mainClass.set("com.google.cloud.functions.invoker.runner.Invoker")
    classpath(invoker)
    inputs.files(configurations.runtimeClasspath, sourceSets.main.get().output)
    args(
        "--target", project.findProperty("run.functionTarget") ?: "" ,
        "--port", project.findProperty("run.port") ?: 8080
    )
    doFirst {
        args("--classpath", files(configurations.runtimeClasspath, sourceSets.main.get().output).asPath)
    }
}