plugins {
    // AGP 9.x 内置 Kotlin 支持；kotlin-android 会重复注册 `kotlin` 扩展，禁止再加
    id("com.android.application") version "9.5.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
}
