import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    testImplementation(libs.kotlin.test)
    testImplementation(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    val puzzleData = rootProject.layout.projectDirectory.file("app/src/main/assets/data.txt")
    inputs.file(puzzleData).withPathSensitivity(PathSensitivity.RELATIVE)
    systemProperty("sudroid.puzzleData", puzzleData.asFile.absolutePath)
}
