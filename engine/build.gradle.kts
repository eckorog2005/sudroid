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

val tools: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

dependencies {
    testImplementation(libs.kotlin.test)
    testImplementation(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.platform.launcher)
}

val puzzleData = rootProject.layout.projectDirectory.file("app/src/main/assets/data.txt")
val generatedPuzzles = rootProject.layout.projectDirectory.file("app/src/main/assets/puzzles.txt")

tasks.test {
    useJUnitPlatform()
    inputs.files(puzzleData, generatedPuzzles).withPathSensitivity(PathSensitivity.RELATIVE)
    systemProperty("sudroid.puzzleData", puzzleData.asFile.absolutePath)
    systemProperty("sudroid.generatedPuzzles", generatedPuzzles.asFile.absolutePath)
}

tasks.register<JavaExec>("generatePuzzles") {
    description = "Appends -Pcount new puzzles per difficulty (default 150) to app/src/main/assets/puzzles.txt."
    classpath = tools.runtimeClasspath
    mainClass = "com.rlamb.android.sudroid.engine.tools.GeneratePuzzlesKt"
    args(puzzleData.asFile, generatedPuzzles.asFile, providers.gradleProperty("count").getOrElse("150"))
}
