import org.jetbrains.compose.desktop.application.tasks.AbstractJPackageTask
import org.jetbrains.compose.desktop.application.tasks.AbstractJLinkTask
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.gradle.api.tasks.JavaExec
import java.nio.file.Files
import java.nio.file.Path

plugins {
    kotlin("jvm") version "2.3.20"
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.github.ben-manes.versions") version "0.53.0"
}

enum class ClassSharingMode {
    None,

    // See https://docs.oracle.com/en/java/javase/26/docs/specs/man/java.html#application-class-data-sharing
    DumpLoadedClasses, CreateArchive, LoadArchive,

    // See https://docs.oracle.com/en/java/javase/26/docs/specs/man/java.html#ahead-of-time-cache
    AotTraining, AotProduction
}

group = "io.github.coderaulia"
version = "1.5.4"
val debugBuild = false
val runMode = ClassSharingMode.None

repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvmToolchain(25)
}

val ffsampledsp by configurations.creating
val ffsampledspVersion = "0.9.54"
val ffsampledspRuntimeDir = layout.buildDirectory.dir("ffsampledsp-runtime")

val prepareFfsampledspRuntime by tasks.registering {
    outputs.dir(ffsampledspRuntimeDir)
    doLast {
        val runtimeDir = ffsampledspRuntimeDir.get().asFile.toPath()
        Files.createDirectories(runtimeDir)
        val expected = runtimeDir.resolve("libbz2.so.1.0")
        if (!Files.exists(expected)) {
            val candidates = listOf(
                Path.of("/usr/lib64/libbz2.so.1.0.8"),
                Path.of("/usr/lib/x86_64-linux-gnu/libbz2.so.1.0.8"),
                Path.of("/lib64/libbz2.so.1.0.8"),
            )
            val source = candidates.firstOrNull { Files.exists(it) }
            if (source != null) {
                Files.createSymbolicLink(expected, source)
            }
        }
    }
}

tasks.withType<JavaExec>().configureEach {
    dependsOn(prepareFfsampledspRuntime)
    doFirst {
        val runtimeDir = ffsampledspRuntimeDir.get().asFile.absolutePath
        val existing = environment["LD_LIBRARY_PATH"]?.toString()
        environment["LD_LIBRARY_PATH"] = listOfNotNull(runtimeDir, existing).joinToString(":")
    }
}

dependencies {
    implementation("org.jetbrains.compose.foundation:foundation:1.10.3")
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.components:components-resources:1.10.3")
    implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
    implementation("org.jetbrains.compose.material3:material3:1.9.0")
    val coroutine = "1.10.2"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutine")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:$coroutine")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:$coroutine")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")

    // Portals
    implementation("com.github.MMarco94:klib-portal:0.3")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:8.0.01")
    implementation("org.tinylog:tinylog-impl:2.7.0")
    implementation("org.tinylog:slf4j-tinylog:2.7.0")
    implementation("org.slf4j:jul-to-slf4j:2.0.17")

    // ffmpeg-based audio decoder
    val os = org.gradle.internal.os.OperatingSystem.current()
    val arch = System.getProperty("os.arch")
    val ffsampledspArtifact = when {
        os.isMacOsX && arch == "aarch64" -> "ffsampledsp-aarch64-macos"
        os.isMacOsX -> "ffsampledsp-x86_64-macos"
        os.isLinux && arch == "aarch64" -> "ffsampledsp-aarch64-linux"
        os.isLinux && arch == "amd64" -> "ffsampledsp-x86_64-linux"
        os.isWindows && arch.contains("64") -> "ffsampledsp-x86_64-win"
        os.isWindows -> "ffsampledsp-i386-win"
        else -> error("Unsupported platform: $os $arch")
    }
    val ffsampledspType = when {
        os.isMacOsX -> "dylib"
        os.isLinux -> "so"
        os.isWindows -> "dll"
        else -> error("Unsupported OS: $os")
    }
    ffsampledsp("com.tagtraum:$ffsampledspArtifact:$ffsampledspVersion@$ffsampledspType")
    implementation("com.tagtraum:ffsampledsp-java:$ffsampledspVersion")
    // music metadata reader
    implementation("net.jthink:jaudiotagger:3.0.1")
    implementation("com.github.bjoernpetersen:m3u-parser:1.4.0")
    // DBUS APIs
    implementation("com.github.hypfvieh:dbus-java-core:5.2.0")
    implementation("com.github.hypfvieh:dbus-java-transport-native-unixsocket:5.2.0")

    val kotest = "6.1.11"
    testImplementation("io.kotest:kotest-runner-junit5:$kotest")
    testImplementation("io.kotest:kotest-assertions-core:$kotest")
    testImplementation("io.kotest:kotest-property:$kotest")
}

fun sanitizeJavaSecurity(file: java.io.File) {
    if (file.exists()) {
        val content = file.readText()
        val sanitized = content.replace(Regex("""(?m)^include redhat/"""), "#include redhat/")
        if (content != sanitized) {
            file.writeText(sanitized)
        }
    }
}

// ffsampledsp can be included in two ways:
//  1. When a JPackage task is present, the .so is copied directly into the destinationDir; it will be loaded at runtime thanks to -Djava.library.path
//  2. When a JPackage task is NOT present, the .so is copied into the app's resources. At runtime, it will be unpacked into /tmp
gradle.taskGraph.whenReady {
    val hasJpackageTask = gradle.taskGraph.allTasks.any { it is AbstractJPackageTask }
    println("has JPackage task = $hasJpackageTask")
    if (hasJpackageTask) {
        tasks.withType(AbstractJPackageTask::class.java).configureEach {
            doLast {
                copy {
                    from(ffsampledsp.files.single())
                    into(destinationDir.dir("MuzikPlayer/lib/app"))
                    rename {
                        System.mapLibraryName("ffsampledsp")
                    }
                }
                sanitizeJavaSecurity(destinationDir.get().asFile.resolve("MuzikPlayer/lib/runtime/conf/security/java.security"))
            }
        }
    } else {
        tasks.processResources {
            from(ffsampledsp) {
                // The file name as expected by FFNativeLibraryLoader doesn't have the version
                // See https://github.com/hendriks73/ffsampledsp/blob/dev/ffsampledsp-complete/pom.xml
                rename {
                    it.replace("-$ffsampledspVersion", "")
                }
            }
        }
    }
}

tasks.withType(AbstractJLinkTask::class.java).configureEach {
    freeArgs.add("--ignore-modified-runtime")
    doLast {
        sanitizeJavaSecurity(destinationDir.get().asFile.resolve("conf/security/java.security"))
    }
}

tasks.named<KotlinCompilationTask<*>>("compileKotlin").configure {
    compilerOptions.optIn.add("kotlin.time.ExperimentalTime")
}

tasks.test {
    useJUnitPlatform()
}

val validateDebugMode by tasks.registering {
    doLast {
        if (debugBuild || runMode != ClassSharingMode.None) {
            throw IllegalArgumentException("Cannot create release build with debug options")
        }
    }
}

tasks.configureEach {
    if (name == "createReleaseDistributable" || name == "proguardReleaseJars") {
        dependsOn(validateDebugMode)
    }
}

compose.desktop {
    application {
        mainClass = "io.github.coderaulia.muzikplayer.MainKt"
        jvmArgs += listOf("--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED")
        jvmArgs += listOf("--enable-native-access=ALL-UNNAMED")
        // To find ffsampledsp.so
        jvmArgs += listOf($$"-Djava.library.path=$APPDIR")
        // These options are set to optimize the memory usage
        jvmArgs += listOf("-XX:+UseZGC") // Use Z Garbage Collector, for low latency, see https://docs.oracle.com/en/java/javase/25/gctuning/z-garbage-collector.html
        jvmArgs += listOf("-XX:SoftMaxHeapSize=256m") // Let's target a reasonable max memory of 256mb
        jvmArgs += listOf("-XX:ZUncommitDelay=1") // Return memory to OS after 1 second
        if (debugBuild) {
            jvmArgs += listOf("-XX:NativeMemoryTracking=summary")
            jvmArgs += listOf("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005")
        }
        when (runMode) {
            ClassSharingMode.None -> {}
            ClassSharingMode.DumpLoadedClasses -> jvmArgs += listOf(
                "-Xshare:off",
                "-XX:DumpLoadedClassList=/tmp/MuzikPlayer.classlist"
            )

            ClassSharingMode.CreateArchive -> jvmArgs += listOf(
                "-Xshare:dump",
                "-XX:SharedClassListFile=/tmp/MuzikPlayer.classlist",
                "-XX:SharedArchiveFile=/tmp/MuzikPlayer.jsa"
            )

            ClassSharingMode.LoadArchive -> jvmArgs += listOf("-XX:SharedArchiveFile=/tmp/MuzikPlayer.jsa")
            ClassSharingMode.AotTraining -> jvmArgs += listOf(
                "-XX:AOTMode=record",
                "-XX:AOTCacheOutput=/tmp/MuzikPlayer.aot"
            )

            ClassSharingMode.AotProduction -> jvmArgs += listOf("-XX:AOTMode=on", "-XX:AOTCache=/tmp/MuzikPlayer.aot")
        }
        nativeDistributions {
            packageName = "MuzikPlayer"
            packageVersion = version.toString()

            modules("java.naming", "java.management", "jdk.security.auth", "jdk.unsupported")
            if (debugBuild) {
                modules.add("jdk.jdwp.agent")
            }
            linux {
                targetFormats(TargetFormat.Deb, TargetFormat.Rpm)
                iconFile.set(project.file("flatpak/icon.png"))
            }
        }
        buildTypes.release.proguard {
            version.set("7.9.0")
            isEnabled.set(providers.gradleProperty("enableProguard").map { it.toBoolean() }.orElse(false))
            optimize = providers.gradleProperty("OptimizeProGuard").orNull != "false"
            obfuscate = false
            configurationFiles.from(project.file("compose-desktop.pro"))
            joinOutputJars = true
        }
    }
}

afterEvaluate {
    tasks.named("createReleaseDistributable") {
        dependsOn(tasks.test)
    }
}

val packagePortableDistributable by tasks.registering(Tar::class) {
    group = "distribution"
    description = "Packages the release distributable into a self-contained tar.gz bundle with user-space installer"
    dependsOn("createReleaseDistributable")

    compression = Compression.GZIP
    archiveBaseName.set("MuzikPlayer")
    archiveVersion.set(version.toString())
    archiveClassifier.set("linux-${System.getProperty("os.arch")}")
    archiveExtension.set("tar.gz")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    into("MuzikPlayer") {
        from(layout.buildDirectory.dir("compose/binaries/main-release/app/MuzikPlayer"))
    }
    into("MuzikPlayer") {
        from("packaging/install.sh") {
            filePermissions { unix("rwxr-xr-x") }
        }
        from("packaging/uninstall.sh") {
            filePermissions { unix("rwxr-xr-x") }
        }
        from("flatpak/icon.svg")
        from("flatpak/icon.png")
    }
}
