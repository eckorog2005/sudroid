// AGP 9.4.1 pulls in vulnerable copies of these build-time tools, and no AGP release ships patched
// ones yet. Drop each pin once AGP's own dependency catches up.
buildscript {
    dependencies {
        constraints {
            classpath(libs.bouncycastle.bcprov)
            classpath(libs.bouncycastle.bcpkix)
            classpath(libs.bouncycastle.bcutil)
            classpath(libs.jose4j)
            classpath(libs.jdom2)
            classpath(libs.commons.lang3)
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}
