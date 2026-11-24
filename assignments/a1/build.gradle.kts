plugins {
    java
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        java.destinationDirectory.set(layout.projectDirectory.dir("bin"))
        resources {
            setSrcDirs(listOf("src"))
            exclude("**/*.java")
        }
        output.setResourcesDir(file("bin"))
    }
    test {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

dependencies {
    implementation(fileTree("lib") {
        include("**/*.jar")
        exclude("jetty-all-*-uber.jar")
    })
}

val port = (findProperty("port") ?: "8081").toString()
val localTest = (findProperty("localtest") ?: "true").toString()

tasks.register<JavaExec>("server") {
    group = "application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.acertainbookstore.server.BookStoreHTTPServer")
    systemProperty("port", port)
}

val bookstoreTest = tasks.register<Test>("bookstoreTest") {
    group = "testing"
    testClassesDirs = sourceSets.main.get().output.classesDirs
    classpath = sourceSets.main.get().runtimeClasspath
    useJUnit()
    filter {
        includeTestsMatching("com.acertainbookstore.client.tests.BookStoreTest")
        includeTestsMatching("com.acertainbookstore.client.tests.StockManagerTest")
    }
    systemProperty("localtest", localTest)
    ignoreFailures = true
    testLogging {
        showStandardStreams = true
        events("passed", "failed")
    }

    // For remote tests, start the server in the background first
    if (localTest == "false") {
        var serverProc: Process? = null
        doFirst {
            val javaBin = "${System.getProperty("java.home")}/bin/java"
            serverProc = ProcessBuilder(
                javaBin,
                "-Dport=$port",
                "-cp", sourceSets.main.get().runtimeClasspath.asPath,
                "com.acertainbookstore.server.BookStoreHTTPServer"
            ).inheritIO().start()
            Thread.sleep(2000) // <sleep seconds="2"/>
        }
        doLast { serverProc?.destroy() }
    }
}

tasks.test {
    enabled = false
    dependsOn(bookstoreTest)
}

tasks.clean {
    delete("bin")
}
