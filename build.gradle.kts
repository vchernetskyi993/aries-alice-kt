plugins {
    kotlin("jvm") version "1.7.20"
    application
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("com.example.AppKt")
}

dependencies {
    implementation("io.javalin:javalin:5.1.1")
    implementation("org.slf4j:slf4j-simple:2.0.3")
    implementation("com.typesafe:config:1.4.2")
    implementation("com.konghq:unirest-java:3.13.11")
}
