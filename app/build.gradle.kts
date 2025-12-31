plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}
android {
    namespace = "com.mviiiax.m8ax_diariogimnasio"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.mviiiax.m8ax_diariogimnasio"
        minSdk = 26
        targetSdk = 34
        versionCode = 101156
        versionName = "10.03.77"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
    packagingOptions {
        resources {
            excludes += setOf(
                "META-INF/NOTICE.md", "META-INF/LICENSE.md", "META-INF/LICENSE-notice.md"
            )
        }
    }
}
dependencies {
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.recyclerview:recyclerview:1.3.1")
    implementation("androidx.room:room-runtime:2.6.0")
    implementation("com.itextpdf:itextg:5.5.10")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("me.zhanghai.android.fastscroll:library:1.3.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.davemorrissey.labs:subsampling-scale-image-view:3.10.0")
    implementation("org.jsoup:jsoup:1.16.1")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("org.shredzone.commons:commons-suncalc:3.5")
    implementation("com.google.zxing:core:3.5.2")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.madgag.spongycastle:core:1.58.0.0")
    implementation("com.madgag.spongycastle:prov:1.58.0.0")
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")
    kapt("androidx.room:room-compiler:2.6.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0")
}