plugins {
    kotlin("jvm")
    id("io.ktor.plugin")
    kotlin("plugin.serialization")
}

group = "com.teamfinder"
version = "1.0.0"

application {
    mainClass.set("com.teamfinder.ApplicationKt")
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Ktor
    implementation("io.ktor:ktor-server-core:2.3.12")
    implementation("io.ktor:ktor-server-netty:2.3.12")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
    implementation("io.ktor:ktor-server-auth:2.3.12")
    implementation("io.ktor:ktor-server-auth-jwt:2.3.12")
    implementation("io.ktor:ktor-server-websockets:2.3.12")
    implementation("io.ktor:ktor-server-cors:2.3.12")
    implementation("io.ktor:ktor-server-status-pages:2.3.12")
    implementation("io.ktor:ktor-server-compression:2.3.12")
    implementation("io.ktor:ktor-server-call-logging:2.3.12")
    // Database
    implementation("org.jetbrains.exposed:exposed-core:0.52.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.52.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.52.0")
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
    
    // Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.6")
    
    // Security
    implementation("org.mindrot:jbcrypt:0.4")
    implementation("com.auth0:java-jwt:4.4.0")
    
    // Testing
    testImplementation("io.ktor:ktor-server-tests:2.3.12")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.24")
}

tasks.withType<Test> {
    useJUnitPlatform()
}