import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "io.github.coderaulia.muzikplayer"
version = "1.5.6"

repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }
    jvm()

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "io.github.coderaulia.muzikplayer.MainKt"
        buildTypes.release.proguard {
            isEnabled.set(false)
        }
        nativeDistributions {
            packageName = "MuzikPlayer"
            packageVersion = version.toString()
            if (org.gradle.internal.os.OperatingSystem.current().isLinux) {
                targetFormats(TargetFormat.Deb, TargetFormat.Rpm)
            }
            if (org.gradle.internal.os.OperatingSystem.current().isWindows) {
                targetFormats(TargetFormat.Msi, TargetFormat.Exe)
                windows {
                    menuGroup = "MuzikPlayer"
                    upgradeUuid = "8a47b7d2-6b7c-4f2d-9b30-7c4a55f2e911"
                }
            }
        }
    }
}
