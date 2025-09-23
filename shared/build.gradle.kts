import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.mavenPublish)
}

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_1_8)
                }
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.compilations.getByName("main") {
            val nskeyvalueobserving by cinterops.creating
        }
    }

    cocoapods {
        summary = "Some description for the Shared Module"
        homepage = "Link to the Shared Module homepage"
        version = "1.0"
        ios.deploymentTarget = "16.0"
        framework {
            baseName = "shared"
            isStatic = true
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
            implementation(compose.runtime)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.guava)

            implementation(libs.koin.android)
            implementation(libs.koin.androidx.workmanager)

            implementation(libs.androidx.media3.common)
            implementation(libs.androidx.media3.session)
            implementation(libs.androidx.media3.exoplayer)

            implementation(libs.androidx.work.runtime.ktx)
            implementation(libs.koin.android)
            implementation(compose.ui)
        }

        iosMain.dependencies {

        }

        all {
            languageSettings.optIn("kotlin.uuid.ExperimentalUuidApi")
        }
    }
}

android {
    namespace = "io.github.moonggae.kmedia"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

mavenPublishing {
    coordinates("io.github.moonggae", "kmedia", libs.versions.publish.get())

    pom {
        name.set("KMedia")
        description.set("Audio player library")
        inceptionYear.set("2025")
        url.set("https://github.com/moonggae/KMedia")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("moonggae")
                name.set("Euigeun Choi")
                url.set("https://github.com/moonggae")
            }
        }
        scm {
            url.set("https://github.com/moonggae/KMedia/")
            connection.set("scm:git:git://github.com/moonggae/KMedia.git")
            developerConnection.set("scm:git:ssh://git@github.com/moonggae/KMedia.git")
        }
    }

    publishToMavenCentral(true)
    signAllPublications()
}