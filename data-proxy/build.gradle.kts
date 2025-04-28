import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    kotlin("jvm") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20"
    id("com.github.ben-manes.versions") version "0.52.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}


val invoker: Configuration by configurations.creating

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("com.google.cloud.functions:functions-framework-api:1.1.4")

    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation(platform("com.google.cloud:libraries-bom:26.59.0"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.4.3")

    testImplementation("com.google.cloud.functions:functions-framework-api:1.1.4")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.12.2")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("io.mockk:mockk:1.14.0")
    testImplementation("org.jetbrains.kotlin:kotlin-reflect")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    invoker("com.google.cloud.functions.invoker:java-function-invoker:1.4.1")
}


kotlin {
    jvmToolchain(JavaVersion.VERSION_21.majorVersion.toInt())
}

tasks.withType<DependencyUpdatesTask> {
    val preReleaseVersion = "^.*(rc[-.]?\\d*|m\\d+|-[Bb]eta-?\\d*|-alpha-?\\d*)$".toRegex(RegexOption.IGNORE_CASE)
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
