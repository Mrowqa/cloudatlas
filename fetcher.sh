#!/bin/sh

dir=${PWD}
rundir=${dir}/build/classes
path=${rundir}:${dir}/lib/cup.jar:${dir}/lib/JLex.jar

policy='grant codeBase "file:'
policy=$policy$rundir
suffix='" {\n permission java.security.AllPermission;\n};'
policy=$policy$suffix
echo $policy > ${rundir}/all.policy

java -cp ${path} -Djava.rmi.server.codebase=file:${rundir} \
  -Djava.security.policy=all.policy pl.edu.mimuw.cloudatlas.fetcher.Main $@
