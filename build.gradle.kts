plugins {
    id("org.gradle.playframework") version "0.12"
    idea
}

repositories {
    mavenCentral()
    maven {
        name = "lightbend-maven-releases"
        url = uri("https://repo.lightbend.com/lightbend/maven-release")
    }
    ivy {
        name = "lightbend-ivy-release"
        url = uri("https://repo.lightbend.com/lightbend/ivy-releases")
        layout("ivy")
    }
}

play {
    platform {
        playVersion.set(property("play.version") as String)
        scalaVersion.set(property("scala.version") as String)
        javaVersion.set(JavaVersion.VERSION_17)
    }
    injectedRoutesGenerator.set(true)
}

dependencies {
    implementation("org.slf4j:slf4j-simple:${property("slf4j-simple.version")}")
    implementation("org.eclipse.jgit:org.eclipse.jgit:${property("org.eclipse.jgit.version")}")
    implementation("com.typesafe.play:play-guice_2.13:${property("play.version")}")
    implementation("com.typesafe.play:filters-helpers_2.13:${property("play.version")}")
    implementation("net.codingwell:scala-guice_2.13:${property("scala-guice.version")}")
    implementation("org.scalaz:scalaz-concurrent_2.13:${property("scalaz.version")}")

    testImplementation("org.scalatestplus.play:scalatestplus-play_2.13:${property("scalatestplus-play_2.13.version")}")
    testImplementation("org.scalatestplus:junit-4-13_2.13:${property("scalatestplus-junit-4-13.version")}")
}
