plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

application {
    mainClass.set("com.tiendavirtual.gateway.MainKt")
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.slf4j.simple)
    testImplementation(libs.junit.jupiter)
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "com.tiendavirtual.gateway.MainKt"
        )
    }
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveBaseName.set("API-Gateway")
    archiveClassifier.set("")
    archiveVersion.set("1.0.0")
    manifest {
        attributes(
            "Main-Class" to "com.tiendavirtual.gateway.MainKt"
        )
    }
}

tasks.named("distZip") { dependsOn(tasks.named("shadowJar")) }
tasks.named("distTar") { dependsOn(tasks.named("shadowJar")) }
tasks.named("startScripts") { dependsOn(tasks.named("shadowJar")) }
tasks.named("startShadowScripts") { dependsOn(tasks.named("jar")) }
