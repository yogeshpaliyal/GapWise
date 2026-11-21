plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "com.yogeshpaliyal.gapwise"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.yogeshpaliyal.gapwise"
        minSdk = 26
        targetSdk = 36
        // Bumped by the "Release" GitHub workflow; don't edit by hand unless you mean it.
        versionCode = 8
        versionName = "0.4.0"
        // The commit this was built from (CI only). Debug builds compare it with latest-master.
        buildConfigField("String", "GIT_SHA", "\"${System.getenv("GITHUB_SHA") ?: ""}\"")
    }

    // Release signing comes from env vars (set by CI from GitHub secrets, or exported locally).
    // Without SIGNING_KEYSTORE_PATH the release build is left unsigned instead of failing.
    val releaseKeystore: String? = System.getenv("SIGNING_KEYSTORE_PATH")
    signingConfigs {
        // Committed on purpose so every machine and CI build signs debug APKs with the same
        // key, which lets new builds install over old ones. Never used for releases.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        // Debug installs next to the Play/release app: its own package and name.
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // Debug builds track the rolling latest-master pre-release.
            buildConfigField("String", "UPDATE_CHANNEL", "\"latest-master\"")
        }
        release {
            // Release builds track the newest proper GitHub Release (vX.Y.Z).
            buildConfigField("String", "UPDATE_CHANNEL", "\"release\"")
            if (releaseKeystore != null) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            // The commit hash AGP would record isn't the tag commit when CI builds (the version
            // bump is committed after the build), which would break reproducible builds.
            vcsInfo.include = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Where the app is distributed decides how it updates itself.
    //  - github: sideloaded APK from GitHub releases; checks GitHub for newer builds.
    //  - play:   Google Play; uses Play In-App Updates and In-App Review. No self-update code.
    //  - fdroid: F-Droid and IzzyOnDroid. Free software only (no Google Play services, so no
    //            Drive backup) and no self-updater. Built reproducibly: F-Droid rebuilds it from
    //            the tag and ships our signed APK only if its build matches byte for byte.
    flavorDimensions += "store"
    productFlavors {
        create("github") {
            dimension = "store"
            isDefault = true
            buildConfigField("String", "GITHUB_REPO", "\"yogeshpaliyal/Gapwise\"")
            buildConfigField("boolean", "HAS_DRIVE", "true")
        }
        create("play") {
            dimension = "store"
            buildConfigField("boolean", "HAS_DRIVE", "true")
        }
        create("fdroid") {
            dimension = "store"
            buildConfigField("boolean", "HAS_DRIVE", "false")
            // Nothing that depends on the build machine may end up in this APK.
            buildConfigField("String", "GIT_SHA", "\"\"")
            proguardFile("proguard-fdroid.pro")
        }
    }
    // Google-only code (Drive backup) is shared by the github and play flavors.
    sourceSets {
        for (flavor in listOf("github", "play")) {
            getByName(flavor).java.srcDir("src/gms/java")
            getByName(flavor).kotlin.srcDir("src/gms/java")
        }
    }

    // F-Droid rejects Google's encrypted dependency block, and it isn't needed elsewhere either.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        // Print the full stack trace and the app's log when a test fails in CI.
        unitTests.all {
            it.testLogging {
                events("failed")
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
                showStandardStreams = true
            }
            it.systemProperty("robolectric.logging", "stdout")
            // A fresh JVM per test class: Compose roots from one class's activities can linger
            // into the next, so screens from an earlier test show up in a later one.
            it.forkEvery = 1
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// Room's Gradle plugin keeps one schema per variant and merges them, so building several
// flavors at once can't corrupt the exported schema file.
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.graphics.shapes)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.datastore.preferences)
    "githubImplementation"(libs.play.services.auth)
    "playImplementation"(libs.play.services.auth)
    implementation(libs.kotlinx.serialization.json)

    "playImplementation"(libs.play.app.update)
    "playImplementation"(libs.play.review)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.withType<Test>().configureEach {
    testLogging {
        events("passed", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStackTraces = true
    }
}

// Reproducible F-Droid builds: the baseline profile (assets/dexopt) isn't byte-for-byte stable
// between machines, so the fdroid flavor ships without it. The other flavors keep it.
tasks.configureEach {
    if (name.contains("Fdroid") && name.contains("ArtProfile")) enabled = false
}
