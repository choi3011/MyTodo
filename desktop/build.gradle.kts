import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.swing)
}

compose.desktop {
    application {
        mainClass = "com.example.mytodo.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "MyTodo"
            packageVersion = "1.0.6"
            description = "MyTodo desktop client"
            vendor = "choi3011"

            modules(
                "java.net.http",
                "jdk.httpserver",
                "java.naming",
                "java.management",
                "jdk.unsupported",
            )

            windows {
                iconFile.set(project.file("src/main/resources/icon.ico"))
                menuGroup = "MyTodo"
                shortcut = true
                dirChooser = true
                upgradeUuid = "9b3a52a4-d1e3-4f77-ae21-8e2c0f5b9d10"
            }
            macOS {
                iconFile.set(project.file("src/main/resources/icon.png"))
            }
            linux {
                iconFile.set(project.file("src/main/resources/icon.png"))
            }
        }
    }
}
