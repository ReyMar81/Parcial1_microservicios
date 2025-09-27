pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ventas-microservices-mvp"
// SOLO módulos necesarios para los 5 casos de uso
include(":App-Administrador")
include(":API-Gateway")
include(":microservicios:Productos")
include(":microservicios:Clientes")
include(":microservicios:Ventas")
