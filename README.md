![KairosDB](webroot/img/kairosdb.png)
[![Build](https://github.com/waylayio/kairosdb/actions/workflows/build.yml/badge.svg?branch=2.0.0-waylay)](https://github.com/waylayio/kairosdb/actions/workflows/build.yml?query=branch%3A2.0.0-waylay)

KairosDB is a fast distributed scalable time series database written on top of Cassandra.

## Documentation

Documentation is found [here](http://kairosdb.github.io/website/).

[Frequently Asked Questions](https://github.com/kairosdb/kairosdb/wiki/Frequently-Asked-Questions)

## Installing

Download the latest [KairosDB release](https://github.com/kairosdb/kairosdb/releases).

Installation instructions are found [here](http://kairosdb.github.io/docs/build/html/GettingStarted.html)

If you want to test KairosDB in Kubernetes please follow the instructions from [KairosDB Helm chart](deployment/helm/README.md).

## Getting Involved

Join the [KairosDB discussion group](https://groups.google.com/forum/#!forum/kairosdb-group).

## Building and releasing WAYLAY branch

The Waylay branch is built using maven. For 2.0.0.x versions do checkoud of `2.0.0-waylay` branch before release:

    $ git checkout 2.0.0-waylay

Note that the version number includes a patch number (e.g. the "+3" in 2.0.0-waylay+3). This patch number
should be incremented on each release (and the base KairosDB version should remain unchanged).

To run tests using a local Cassandra installation, run

    $ CASSANDRA_HOST=localhost mvn clean test

Running `mvn test` without the `CASSANDRA_HOST` environment variable will skip tests that depend on Cassandra.

Running `mvn deploy` will push the artifacts, including the distributable .tar.gz to Nexus
(make sure you have specified credentials for NEXUS `maven-releases` server in settings.xml)


Releases can be performed with the following command (TODO:make sure that the release pushes the artifacts to nexus):

    mvn clean -DskipTests -Darguments=-DskipTests -Dmaven.javadoc.skip=true  release:clean release:prepare release:perform

Note that the `build` directory which is created by a number of tests doesn't always get cleaned up properly by
the tests. This means that it may be necessary to do a `rm -rf build` between builds of this repo.

## Contributing to KairosDB

Contributions to KairosDB are **very welcome**. KairosDB is mainly developed in Java, but there's a lot of tasks for non-Java programmers too, so don't feel shy and join us!

What you can do for KairosDB:

- [KairosDB Core](https://github.com/kairosdb/kairosdb): join the development of core features of KairosDB.
- [Website](https://github.com/kairosdb/kairosdb.github.io): improve the KairosDB website.
- [Documentation](https://github.com/kairosdb/kairosdb/wiki/Contribute:-Documentation): improve our documentation, it's a very important task.

If you have any questions about how to contribute to KairosDB, [join our discussion group](https://groups.google.com/forum/#!forum/kairosdb-group) and tell us your issue.

## License
The license is the [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0)
