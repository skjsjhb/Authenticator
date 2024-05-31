plugins {
    java
    application
}

val mainClazz = "skjsjhb.mc.auth.Authenticator"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.jar {
    archiveBaseName = "${project.name}-app"
    manifest {
        attributes["Main-Class"] = mainClazz
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

repositories {
    mavenCentral()
}

application {
    mainClass = mainClazz
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        applicationDefaultJvmArgs = listOf(
            "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
            "--add-opens=java.desktop/sun.lwawt=ALL-UNNAMED",
            "--add-opens=java.desktop/sun.lwawt.macosx=ALL-UNNAMED"
        )
    }
}

dependencies {
    implementation("me.friwi:jcefmaven:122.1.10")
}
