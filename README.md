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

First, you have to add a set of env variables:

| variable                | description     | example.                                       |
|-------------------------|-----------------|------------------------------------------------|
|GITLAB_URL               |url of the gitlab|"https://gitlab.ekino.com"                      |
|GITLAB_USER              |gitlab user      |"philippe.agra"                                 |
|GITLAB_TOKEN             |gitlab api token |"XxXxXxXxXxXxXxXxXxXx"                          |
|GITLAB_GROUP_IDS         |gitlab group ids |"1524,626"                                      |
|EKINO_REPOSITORY_URL     |nexus url        |"https://nexus.ekino.com/repository/public-mfg/"|
|EKINO_REPOSITORY_USER    |nexus user.      |"philippe.agra"                                 |
|EKINO_REPOSITORY_PASSWORD|nexus password   |"XxXxXxXx"                                      |


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
Test the application on url http://localhost:9000/


### Run in docker mode
```bash
$ ./gradlew clean dist
$ docker build . -t ekino-tools-version
$ docker run -d -p 8080:8080 -v /tmp/versions:/tmp/versions ekino-tools-version
```
Test the application on url http://localhost:8080/


## Documentation

* https://www.playframework.com/documentation/2.6.x/Home
* http://www.scala-lang.org/
* http://akka.io/docs/
