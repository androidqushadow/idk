
group = "org.Krisp.re"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

plugins {
    kotlin("multiplatform") version "2.3.10"
}

kotlin {
//    jvmToolchain(21)
    mingwX64("windows") {
        binaries {
            executable {
                baseName = "Krisp"
                entryPoint = "org.Krisp.re.main"
            }
        }
    }


    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.3.0")
                implementation("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.3.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
            }
        }
    }
}
