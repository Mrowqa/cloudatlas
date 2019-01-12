#!/bin/sh
# Argument $1 - interval of query executer: <number>h|m|s

dir=${PWD}
rundir=${dir}/build/classes
path=${rundir}:${dir}/lib/cup.jar:${dir}/lib/JLex.jar:${dir}/lib/guava-23.0.jar:${dir}/lib/json.jar

./gen-policy-file.sh ${rundir}

if [ "$1" = "--config-file" ]; then
  key=$(python -c "import json; print(json.load(open('${2}'))['pubKeyFilename'])");
  cp ${key} ${rundir}
  cp ${2} ${rundir}
fi

cd build/classes && java -cp ${path} -Djava.rmi.server.codebase=file:${rundir} \
  -Djava.rmi.server.hostname=localhost -Djava.security.policy=all.policy \
  pl.edu.mimuw.cloudatlas.agent.Main $@
