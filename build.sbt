import java.util.Properties

name := "ekino-tools-version"
version := "1.0.0"
scalaVersion := "2.13.5"

val gradleProperties = settingKey[Properties]("The gradle properties")
gradleProperties := {
  val prop = new Properties()
  IO.load(prop, new File("gradle.properties"))
  prop
}

libraryDependencies += guice
libraryDependencies += filters
libraryDependencies += "org.slf4j"               % "slf4j-simple"            % gradleProperties.value.getProperty("slf4j-simple.version")
libraryDependencies += "org.eclipse.jgit"        % "org.eclipse.jgit"        % gradleProperties.value.getProperty("org.eclipse.jgit.version")
libraryDependencies += "net.codingwell"         %% "scala-guice"             % gradleProperties.value.getProperty("scala-guice.version")
libraryDependencies += "org.scalaz"             %% "scalaz-concurrent"       % gradleProperties.value.getProperty("scalaz.version")

libraryDependencies += specs2                    % Test
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play"      % gradleProperties.value.getProperty("scalatestplus-play_2.13.version") % Test
libraryDependencies += "org.scalatestplus"      %% "junit-4-13"              % gradleProperties.value.getProperty("scalatestplus-junit-4-13.version") % Test

routesGenerator := InjectedRoutesGenerator


lazy val `ekino-tools-version` = (project in file(".")).enablePlugins(PlayScala)
