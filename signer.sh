#!/bin/sh

dir=${PWD}
rundir=${dir}/build/classes
path=${rundir}:${dir}/lib/cup.jar:${dir}/lib/JLex.jar:${dir}/lib/guava-23.0.jar:${dir}/lib/sqlite-jdbc-3.23.1.jar

./gen-policy-file.sh ${rundir}

if [ "$2" != "" ]; then key=$2; else key=private_key.der; fi
cp ${key} ${rundir}
if [ "$4" != "" ]; then db=$4; else db=signer.db; fi
touch ${db}  # make sure it does exist
ln -f ${db} ${rundir}/${db}  # assuming it's relative path; absolute will work, too

hostname=$(hostname)
echo "Info: using host \"${hostname}\" for RMI server (you can edit this script to change it)"

cd build/classes && java -cp ${path} -Djava.rmi.server.codebase=file:${rundir} \
  -Djava.rmi.server.hostname=${hostname} -Djava.security.policy=all.policy \
  pl.edu.mimuw.cloudatlas.signer.Main $@

