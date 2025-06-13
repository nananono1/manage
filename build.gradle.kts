plugins {
    alias(libs.plugins.android.application) apply false
}

buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.1")
    }
}
