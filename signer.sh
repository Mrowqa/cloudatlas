#!/bin/sh

dir=${PWD}
rundir=${dir}/build/classes
path=${rundir}:${dir}/lib/cup.jar:${dir}/lib/JLex.jar:${dir}/lib/guava-23.0.jar

./gen-policy-file.sh ${rundir}

if [ "$2" != "" ]; then key=$2; else key=private_key.der; fi
cp ${key} ${rundir}

cd build/classes && java -cp ${path} -Djava.rmi.server.codebase=file:${rundir} \
  -Djava.rmi.server.hostname=localhost -Djava.security.policy=all.policy \
  pl.edu.mimuw.cloudatlas.signer.Main $@
