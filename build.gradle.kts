import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.codingfeline.buildkonfig.gradle.*
import com.codingfeline.buildkonfig.compiler.*
import java.io.ByteArrayOutputStream

plugins {
    application
    id("org.jetbrains.kotlin.multiplatform") version "1.3.71"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.3.71"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("com.codingfeline.buildkonfig") version "0.5.1"
}

repositories {
    jcenter()
    maven("https://dl.bintray.com/kotlin/ktor")
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
    maven("https://kotlin.bintray.com/kotlin-js-wrappers/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    mavenCentral()
}

application {
    mainClassName = "ticketToRide.ServerKt"
}

val ktor_version = "1.3.2"
val kotlinx_html_version = "0.7.1"
val serialization_version = "0.20.0"
val kotest_version = "4.1.0.268-SNAPSHOT"

kotlin {
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    js {
        browser {}
        useCommonJs()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$serialization_version")
                implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.2")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation("org.jetbrains:kotlin-css:1.0.0-pre.94-kotlin-1.3.70")
                implementation("org.jetbrains:kotlin-css-jvm:1.0.0-pre.94-kotlin-1.3.70")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.5")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serialization_version")
                implementation("io.ktor:ktor-server-core:$ktor_version")
                implementation("io.ktor:ktor-server-netty:$ktor_version")
                implementation("io.ktor:ktor-serialization:$ktor_version")
                implementation("io.ktor:ktor-html-builder:$ktor_version")
                implementation("io.ktor:ktor-websockets:$ktor_version")
                implementation("io.github.microutils:kotlin-logging:1.7.9")
                implementation("org.slf4j:slf4j-simple:1.7.29")
            }
            languageSettings.apply {
                useExperimentalAnnotation("kotlinx.coroutines.ExperimentalCoroutinesApi")
                useExperimentalAnnotation("kotlinx.coroutines.FlowPreview")
                useExperimentalAnnotation("io.ktor.util.KtorExperimentalAPI")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("io.kotest:kotest-runner-console-jvm:$kotest_version")
                implementation("io.kotest:kotest-assertions-core-jvm:$kotest_version")
                implementation("io.kotest:kotest-property-jvm:4.0.5")
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
                implementation(npm("react", "16.13.0"))
                implementation(npm("react-dom", "16.13.0"))
                implementation("org.jetbrains.kotlinx:kotlinx-html-js:$kotlinx_html_version")
                implementation("org.jetbrains:kotlin-styled:1.0.0-pre.94-kotlin-1.3.70")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$serialization_version")
                implementation("com.ccfraser.muirwik:muirwik-components:0.4.1")
                implementation(npm("@material-ui/core", "4.9.8"))
                implementation(npm("styled-components", "5.0.1"))
                implementation(npm("inline-style-prefixer", "6.0.0"))
                implementation(npm("google-map-react", "1.1.7"))
            }
        }
        val jsTest by getting {
            dependencies {
                implementation("io.kotest:kotest-core-js:$kotest_version")
                implementation("io.kotest:kotest-assertions-core-js:$kotest_version")
                implementation("io.kotest:kotest-property-js:$kotest_version")
            }
        }
    }

    kotlin.sourceSets.all {
        languageSettings.apply {
            useExperimentalAnnotation("kotlin.ExperimentalStdlibApi")
            useExperimentalAnnotation("kotlinx.serialization.UnstableDefault")
        }
    }
}

tasks {
    configure<BuildKonfigExtension> {
        fun getGitHash(): String {
            val versionFromEnv = System.getenv("SOURCE_VERSION")
            if (versionFromEnv != null)
                return versionFromEnv

            val stdout = ByteArrayOutputStream()
            exec {
                commandLine("git", "rev-parse", "--short", "HEAD")
                standardOutput = stdout
            }
            return stdout.toString().trim()
        }

        packageName = "ticketToRide"
        defaultConfigs {
            buildConfigField(FieldSpec.Type.STRING, "version", getGitHash())
        }
    }

    val devJs = named<KotlinWebpack>("jsBrowserDevelopmentWebpack")
    val prodJs = named<KotlinWebpack>("jsBrowserProductionWebpack")

    named<ShadowJar>("shadowJar") {
        manifest {
            attributes("Main-Class" to application.mainClassName)
        }
        archiveFileName.set("ticket-to-ride.fat.jar")
        val jvmCompilation = kotlin.jvm().compilations["main"]
        configurations = mutableListOf(jvmCompilation.compileDependencyFiles as Configuration)
        from(jvmCompilation.output)
        dependsOn(prodJs)
        from(prodJs.get().outputFile)
    }

    val prodJar = create<Jar>("productionJar") {
        archiveFileName.set("ticket-to-ride.prod.jar")
        dependsOn(prodJs)
        from(prodJs.get().outputFile)
    }

    val devJar = create<Jar>("developmentJar") {
        archiveFileName.set("ticket-to-ride.dev.jar")
        dependsOn(devJs)
        from(devJs.get().outputFile)
    }

    configure(listOf(prodJar, devJar)) {
        from(kotlin.jvm().compilations["main"].output)
        manifest {
            attributes("Main-Class" to "ticketToRide.ServerKt")
        }
    }

    create<JavaExec>("runProd") {
        group = "application"
        main = "ticketToRide.ServerKt"
        dependsOn(prodJar)
        classpath(configurations["jvmRuntimeClasspath"], prodJar)
    }

    create<JavaExec>("runDev") {
        group = "application"
        main = "ticketToRide.ServerKt"
        dependsOn(devJar)
        classpath(configurations["jvmRuntimeClasspath"], devJar)
    }
}