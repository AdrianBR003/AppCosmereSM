plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("kotlin-kapt")
}

android {
    namespace = "com.example.appsandersonsm"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.appsandersonsm"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.0-beta"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        abortOnError = false // Esto asegura que Lint no detenga el proceso de construcción
        warningsAsErrors = false // Opcional, para no tratar advertencias como errores
    }
}

dependencies {

    // NewAPI & RetroFit
    implementation (libs.retrofit)
    implementation (libs.converter.gson)
    implementation (libs.glide)
    implementation(libs.firebase.crashlytics.buildtools)
    annotationProcessor (libs.compiler)



    // ROOM
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx.v261)
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")




    implementation(libs.glide.v4120)
    implementation(libs.glide.transformations)

    // Firebase BOM (define versiones centralizadas para Firebase)
    implementation(platform(libs.firebase.bom))

    // Firebase y Google Sign-In
    implementation(libs.firebase.auth.ktx) // Se gestiona a través de Firebase BOM
    implementation(libs.play.services.auth) // Verifica la versión correcta

    // Otras dependencias
    implementation(libs.play.services.auth)
    implementation(libs.firebase.analytics)
    implementation(libs.core.splashscreen)
    implementation(libs.labs.subsampling.scale.image.view)
    implementation(libs.glide)
    implementation(libs.getstream.photoview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat.v161)
    implementation(libs.materialratingbar.library)


    implementation(platform(libs.androidx.compose.bom))

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Debug dependencies
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
