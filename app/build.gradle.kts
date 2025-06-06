plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.asistencia_comida"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.asistencia_comida"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}

dependencies {

    //dependencia de volley
    implementation ("com.android.volley:volley:1.2.1")

    //Dependencias de escaneo de barra
    implementation ("com.journeyapps:zxing-android-embedded:4.2.0")
    implementation("com.google.zxing:core:3.3.0")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}