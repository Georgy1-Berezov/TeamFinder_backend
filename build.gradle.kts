plugins {
    kotlin("multiplatform") version "1.9.24" apply false
    kotlin("jvm") version "1.9.24" apply false
    kotlin("plugin.serialization") version "1.9.24" apply false
    id("com.android.application") version "8.3.2" apply false
    id("com.android.library") version "8.3.2" apply false
    id("org.jetbrains.compose") version "1.6.11" apply false
    id("io.ktor.plugin") version "2.3.12" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
    
    // Явно указываем Java 17 для всех задач компиляции
    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}