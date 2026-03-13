plugins {
    id 'org.jetbrains.kotlin.jvm' version '1.9.20'
    id 'application'
}

group = 'gradlepretty'
version = '1.0.0'

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.jetbrains.kotlin:kotlin-stdlib:1.9.20'
}

application {
    mainClass = 'gradlepretty.GradlePrettyKt'
}

kotlin {
    jvmToolchain(17)
}

tasks.register('run', JavaExec) {
    group = 'application'
    description = 'Run the application'
    classpath = sourceSets.main.runtimeClasspath
    mainClass = 'gradlepretty.GradlePrettyKt'
    standardInput = System.in
}