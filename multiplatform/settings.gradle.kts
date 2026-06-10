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
        // ที่อยู่ของ usb-serial-for-android (ฝั่ง Android)
        maven { url = uri("https://jitpack.io") }
    }
}
rootProject.name = "LgTvControllerCMP"
include(":composeApp")
