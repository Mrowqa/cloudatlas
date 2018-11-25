CloudAtlas
==========

Poor man's version of AstroLabe for Distributed Systems class.


Build
-----

To build, run::

    $ ant

Please note it can say that `javadoc` failed to build, but we don't use it
anyway. What's important - the code builds just fine.

The other option is to build the project using NetBeans IDE 9.


Envinronment
------------

Tested on Ubuntu 18.10 with Oracle Java 1.8.


Running the system (sripts)
---------------------------

All scripts should be run from the root directory (where the scripts are).

First, run the rmi registry with::

    $ ./registry.sh

Then, run the agent::

    $ ./server.sh --sleep 5m

You can an extra argument to configure how often Agent executes installed
queries. Default is 5 minutes.

### Extra scripts

#### Fetcher

Fetcher fetches data from local OS and feeds specified zone in agent running
on localhost::

    $ ./fetcher.sh --sleep 5m --zone /uw/violet07

sleep: how often fetch data and set them in agent, default 5 minutes
zone: the specified zone that fetcher feeds data to

#### WebClient

WebClient provides a web interface to interact with CloudAtlas. It does also
store historical data, for the charts. It runs at localhost:8000.

    $ ./webclient.sh --sleep 5m

sleep: how often fetch data for history charts, default 5 minutes

#### CLI

Simple command line interface::

    $ ./client.sh

#### Standalone query interpreter

You can run standalone query interpreter with::

    $ ./interpreter.sh [--test]

By default, it uses specified test hierarchy in assignment 1. By adding
`--test` argument, it uses hierarchy from scenario 3, instead.

