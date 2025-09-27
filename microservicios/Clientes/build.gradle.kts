plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

application {
    mainClass.set("com.tiendavirtual.clientes.MainKt")
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.hikaricp)
    implementation(libs.postgresql)
    implementation(libs.slf4j.simple)
    testImplementation(libs.junit.jupiter)
}

// Manifest para jar normal (lo volvemos a habilitar)
tasks.jar {
    enabled = true
    manifest {
        attributes(
            "Main-Class" to "com.tiendavirtual.clientes.MainKt"
        )
    }
}

// Shadow jar original (fat jar)
tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveBaseName.set("Clientes")
    archiveClassifier.set("")
    archiveVersion.set("1.0.0")
    manifest {
        attributes(
            "Main-Class" to "com.tiendavirtual.clientes.MainKt"
        )
    }
}

tasks.named("distZip") { dependsOn(tasks.named("shadowJar")) }
tasks.named("distTar") { dependsOn(tasks.named("shadowJar")) }
tasks.named("startScripts") { dependsOn(tasks.named("shadowJar")) }
tasks.named("startShadowScripts") { dependsOn(tasks.named("jar")) }
