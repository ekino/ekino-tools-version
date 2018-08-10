# Ekino Tools Versions

## Description

Tool that provides convenient version management webapp.
It is based on Play framework, using activator, sbt, akka and Scala.

It provides 4 views:
* Main view: list of the repositories
* Repository Details: all the versions and plugins of a given repository
* Dependency Details: all the repositories using the dependency
* Plugin Details: all the repositories using the plugin (only for gradle )


## Requirements

JDK 8 is required.

## IDE

The IntelliJ integration is not automatic.
A Scala SDK needs to be added to the project.
This Gradle command has to be launched manually:
```
$ ./gradlew idea
```

## Run

### Run in dev mode

First, you have to change some properties in `application.conf`.

The `project.repositories.path` property should be set with the directory containing the Git repositories on your computer. If not preceded by `/`, it is relative to the current directory.

Then add the Maven credentials.

For example:
```
project.repositories.path = "projets/ekino/workspace"
maven.repository.user = "my.user"   # replace by real credentials 
maven.repository.password = "1234"
```

Then run the application with Gradle
```
./gradlew -t runPlayBinary
```
Test the application on url http://localhost:9000/ekino-tools-version/

### Run in binary mode
```bash
$ ./gradlew build
$ unzip build/distributions/playBinary.zip -d build/distributions
$ ./build/distributions/playBinary/bin/playBinary
```
Test the application on url http://localhost:9000/ekino-tools-version/


### Run in docker mode
```bash
$ ./gradlew buildDocker
$ docker run -d -p 8080:8080 -v $HOME/ekino:/root/ekino ekino-tools-version:1.0.0-SNAPSHOT
```
Test the application on url http://localhost:8080/ekino-tools-version/


## Documentation

* https://www.playframework.com/documentation/2.5.x/Home
* https://www.lightbend.com/community/core-tools/activator-and-sbt
* http://www.scala-lang.org/
* http://akka.io/docs/
