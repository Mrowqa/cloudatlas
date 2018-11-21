#!/bin/sh
# Argument $1 - interval of query executer: <number>h|m|s

dir=${PWD}
rundir=${dir}/build/classes
path=${rundir}:${dir}/lib/cup.jar:${dir}/lib/JLex.jar

policy='grant codeBase "file:'
policy=$policy$rundir
suffix='" {\n permission java.security.AllPermission;\n};'
policy=$policy$suffix
echo ${policy} > ${rundir}/all.policy

cd build/classes && java -cp ${path} -Djava.rmi.server.codebase=file:${rundir} \
  -Djava.rmi.server.hostname=localhost -Djava.security.policy=all.policy \
  pl.edu.mimuw.cloudatlas.agent.Main $1
