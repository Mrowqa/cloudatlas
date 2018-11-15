#!/bin/sh
#
# Distributed Systems Lab
# Copyright (C) Konrad Iwanicki, 2012-2014
#
# This file contains code samples for the distributed systems
# course. It is intended for internal use only.
#

java -cp $PWD:../../lib/cup.jar:../../lib/JLex.jar -Djava.rmi.server.codebase=file:/media/pawel/files1/studia/ds/university-sr-cloudatlas/build/classes -Djava.rmi.server.hostname=localhost -Djava.security.policy=server.policy pl.edu.mimuw.cloudatlas.agent.Main

