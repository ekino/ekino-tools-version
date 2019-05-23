# Ekino Tools Versions

[![Build Status](https://travis-ci.org/ekino/ekino-tools-version.svg?branch=master)](https://travis-ci.org/ekino/ekino-tools-version)

## Description

Tool that provides convenient version management webapp.
It is based on Play framework, akka and Scala.

It provides 5 views:
* Main view: list of the repositories
* Repository Details: all the versions and plugins of a given repository
* Dependencies: list of the dependencies
* Dependency Details: all the repositories using the dependency
* Plugin Details: all the repositories using the plugin


## Requirements

JDK 8 is required.

## Run

### Run in dev mode

First, you have to add a set of env variables:

| Variable name           | description     | example                                        |
|-------------------------|-----------------|------------------------------------------------|
|GITLAB_URL               |url of the gitlab|"https://gitlab.ekino.com"                      |
|GITLAB_USER              |gitlab user      |"philippe.agra"                                 |
|GITLAB_TOKEN             |gitlab api token |"XxXxXxXxXxXxXxXxXxXx"                          |
|GITLAB_GROUP_IDS         |gitlab group ids |"1524,626"                                      |
|GITHUB_USER              |github user      |"philippeagra"                                  |
|GITHUB_TOKEN             |github api token |"XxXxXxXxXxXxXxXxXxXx"                          |
|GITHUB_USERS             |github users     |"ekino,philippeagra"                            |
|LOCAL_REPOSITORY_URL     |nexus url        |"https://nexus.ekino.com/repository/public-mfg/"|
|LOCAL_REPOSITORY_USER    |nexus user       |"philippe.agra"                                 |
|LOCAL_REPOSITORY_PASSWORD|nexus password   |"XxXxXxXx"                                      |


Then run the application with Gradle
```
./gradlew -t runPlay
```
Test the application on url http://localhost:9000/

### Run in binary mode
```bash
$ ./gradlew build
$ unzip build/distributions/ekino-tools-version.zip -d build/distributions
$ ./build/distributions/ekino-tools-version/bin/main
```
Test the application on url http://localhost:9000/


### Run in docker mode
```bash
$ ./gradlew clean dist
$ docker build . -t ekino-tools-version
$ docker run --env-file credentials-docker --rm -p "8080:8080" -v "/tmp/versions:/tmp/versions" ekino-tools-version
```
Test the application on url http://localhost:8080/


## Using sbt
You can also use sbt instead of gradle. It offers a better integration with intelliJ and allows debug.


## Documentation

* https://www.playframework.com/documentation/2.6.x/Home
* http://akka.io/docs/
* http://www.scala-lang.org/
