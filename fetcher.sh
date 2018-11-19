#!/bin/sh
#
# Distributed Systems Lab
# Copyright (C) Konrad Iwanicki, 2012-2014
#
# This file contains code samples for the distributed systems
# course. It is intended for internal use only.
#

#if [ $# -ne 1 ]; then
#  echo "Usage: client.sh <N>" >&2
#  exit 1
#fi

java -cp $PWD:../../lib/cup.jar:../../lib/JLex.jar -Djava.rmi.server.codebase=file:$PWD -Djava.security.policy=client.policy pl.edu.mimuw.cloudatlas.fetcher.Main $@

