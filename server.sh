#!/bin/sh
# Argument $1 - interval of query executer: <number>h|m|s

dir=${PWD}
rundir=${dir}/build/classes
path=${rundir}:${dir}/lib/cup.jar:${dir}/lib/JLex.jar:${dir}/lib/guava-23.0.jar:${dir}/lib/json.jar

./gen-policy-file.sh ${rundir}

if [ "$5" = "--public-key" ]; then key=$6; else key=public_key.der; fi
cp ${key} ${rundir}

if [ "$1" = "--config-file" ]; then cp $2 ${rundir}; fi

cd build/classes && java -cp ${path} -Djava.rmi.server.codebase=file:${rundir} \
  -Djava.rmi.server.hostname=localhost -Djava.security.policy=all.policy \
  pl.edu.mimuw.cloudatlas.agent.Main $@
