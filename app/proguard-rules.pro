# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# RevenueCat
-keep class com.revenuecat.purchases.** { *; }

# DataStore
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# Kotlin Serialization & Coroutines
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# Keep app data models used by Room
-keep class com.example.superinvoice.data.** { *; }
-keep class com.example.superinvoice.data.database.** { *; }

# Firebase Analytics / Crashlytics
# O Crashlytics precisa dos atributos de origem para desobfuscar a stack trace; o
# -keepattributes SourceFile,LineNumberTable acima já cobre isso.
-keepattributes *Annotation*
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Nomes das classes de analytics preservados para os eventos ficarem legíveis no painel
-keep class com.example.superinvoice.data.analytics.** { *; }
