# jb-task
This repository contains my solution for Jetbrains homework task

## Description

There two main modules here:

1. `server` which contains code of ktor application
1. `file-tree-data-provider` which contains pluggable data provider for getting information
about files in specific directory

### Configuration
Base directory (those one on top of which application works) is configured in
[`application.conf`](https://github.com/kozobrodov/jb-task/blob/master/server/src/main/resources/application.conf)
file.

### Build & run

This project can be built by maven:

```
mvn clean package
```

Then it can be run by starting result `jar`:

```
java -jar server/target/server-1.0-SNAPSHOT-jar-with-dependencies.jar
```