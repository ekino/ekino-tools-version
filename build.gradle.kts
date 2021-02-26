plugins {
    id("org.gradle.playframework") version "0.11"
    idea
}

repositories {
    jcenter()
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
        javaVersion.set(JavaVersion.VERSION_1_8)
    }
    injectedRoutesGenerator.set(true)
}

dependencies {
    implementation("org.slf4j:slf4j-simple:${property("slf4j-simple.version")}")
    implementation("org.eclipse.jgit:org.eclipse.jgit:${property("org.eclipse.jgit.version")}")
    implementation("com.typesafe.play:play-guice_2.12:${property("play.version")}")
    implementation("com.typesafe.play:filters-helpers_2.12:${property("play.version")}")
    implementation("net.codingwell:scala-guice_2.12:${property("scala-guice.version")}")
    implementation("org.scalaz:scalaz-concurrent_2.12:${property("scalaz.version")}")

    testImplementation("org.scalatestplus.play:scalatestplus-play_2.12:${property("scalatestplus-play_2.12.version")}")
}
