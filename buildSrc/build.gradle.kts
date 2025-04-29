plugins {
    id("groovy")
    id("org.jetbrains.kotlin.jvm") version "1.8.22"
}

repositories {
    google() // 如果需要依赖 AGP
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
    implementation("com.android.tools.build:gradle:8.1.4")
    implementation("com.alibaba:fastjson:1.2.69")
    // 更新 javapoet 版本
    implementation("com.squareup:javapoet:1.13.0")
    // 更新 commons-codec 版本
    implementation("commons-codec:commons-codec:1.11")
    implementation("commons-io:commons-io:2.4")
}
