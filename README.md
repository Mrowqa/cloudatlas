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


Running the system (scripts)
---------------------------

All scripts should be run from the root directory (where the scripts are).

First, run the rmi registry with::

    $ ./registry.sh

Then, run the agent::

    $ ./server.sh --config-file path/to/config/file.conf

There are some test configs included in this package. Look for `*.conf` files.
They are self explanatory.

Agent comes with communication module test::

    $ ./server.sh --test-communication-module

Please note that printed size is size of some field in sent object before
serialization and it is not size of UDP datagrams. You can monitor network
with tools like bmon, though.

### Extra scripts

#### Fetcher

Fetcher fetches data from local OS and feeds specified zone in agent running
on localhost::

    $ ./fetcher.sh --sleep 5m --zone /uw/violet07

sleep: how often fetch data and set them in agent, default 5 seconds
zone: the specified zone that fetcher feeds data to, default /uw/violet07

You can also run it with config file::

    $ ./fetcher.sh --config-file path/to/config/file.conf

#### Query Signer

Signer is a centralized service for signing queries. You can run it with::

    $ ./signer.sh --private-key path/to/private_key.der --database /path/to/signer.db

private-key: key used for signing queries operations,
             default is `private_key.der`
database: signer synchronizes its state with a SQLite database,
          default is `signer.db`

#### WebClient

WebClient provides a web interface to interact with CloudAtlas. It does also
store historical data, for the charts. It runs at localhost:8000.

    $ ./webclient.sh --agent-host <host> --zone /my/leaf/node
    $ ./webclient.sh --config-file /path/to/config/file.conf

agent-host: client connects to this host's registry, then asks it for a remote
            agent stub
zone: name of the remote agent's zone

#### Standalone query interpreter

You can run standalone query interpreter with::

    $ ./interpreter.sh [--test]

By default, it uses specified test hierarchy in assignment 1. By adding
`--test` argument, it uses hierarchy from scenario 3, instead.

