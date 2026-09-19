import java.io.File
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.CommandLineArgumentProvider

class RoomSchemaArgProvider(
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val schemaDir: File,
) : CommandLineArgumentProvider {
    override fun asArguments(): Iterable<String> =
        listOf("room.schemaLocation=${schemaDir.path}")
}

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

val roomSchemaBootstrapDirectory = layout.buildDirectory.dir("room-schema-bootstrap").get().asFile

val prepareRoomSchemaBootstrapDirectory = tasks.register("prepareRoomSchemaBootstrapDirectory") {
    outputs.dir(roomSchemaBootstrapDirectory)
    doLast {
        check(roomSchemaBootstrapDirectory.isDirectory || roomSchemaBootstrapDirectory.mkdirs()) {
            "Unable to create Room schema bootstrap directory: $roomSchemaBootstrapDirectory"
        }
    }
}

tasks.matching { task ->
    task.name.startsWith("ksp") && task.name.endsWith("Kotlin")
}.configureEach {
    dependsOn(prepareRoomSchemaBootstrapDirectory)
}

android {
    namespace = "com.yonte.core.database"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    sourceSets {
        getByName("androidTest").assets.srcDir(File(projectDir, "schemas"))
    }
}

ksp {
    arg(RoomSchemaArgProvider(roomSchemaBootstrapDirectory))
}

dependencies {
    implementation(libs.androidx.core.ktx)
    api(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.sqlcipher.android)
    implementation(libs.androidx.sqlite)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.room.testing)
}
