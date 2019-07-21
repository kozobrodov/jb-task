# jb-task
This repository contains my solution for Jetbrains homework task

## Description

There two main modules here:

1. `server` which contains code of ktor application
1. `file-tree-data-provider` which contains pluggable data provider for getting information
about files in specific directory

Since these modules use the same test data for tests, this test data resides in a separate
directory [test-data](https://github.com/kozobrodov/jb-task/tree/master/test-data/).

There is also a [frontend](https://github.com/kozobrodov/jb-task/tree/master/frontend) directory
which contains code of JQuery [plugin](https://github.com/kozobrodov/jb-task/blob/master/frontend/js/fileTree.js)
for displaying file tree, appropriate styles, fonts, etc and three test pages:

1. [index.html](https://github.com/kozobrodov/jb-task/blob/master/frontend/index.html) uses
`JsonDataProvider`, so it's possible to test plugin without any client-server communication
— all logic is "in-browser"
1. [service-index.html](https://github.com/kozobrodov/jb-task/blob/master/frontend/service-index.html)
uses `ServiceDataProvider` and expects that server is running on `http://localhost:8081/` host
1. [multiple-trees.index.html](https://github.com/kozobrodov/jb-task/blob/master/frontend/multiple-trees.index.html)
uses both types of data provider and demonstrates that it's possible to have several trees on the
same page

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