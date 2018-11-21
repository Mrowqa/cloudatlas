#!/bin/sh

dir=${PWD}
rundir=${dir}/build/classes
path=${rundir}:${dir}/lib/cup.jar:${dir}/lib/JLex.jar

./gen-policy-file.sh ${rundir}

java -cp ${path} -Djava.rmi.server.codebase=file:${rundir} \
  -Djava.security.policy=all.policy pl.edu.mimuw.cloudatlas.fetcher.Main $@
