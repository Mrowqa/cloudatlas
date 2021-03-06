#!/bin/bash

if [[ "$1" == "" ]]; then
    echo "Usage: $0 <rundir>" >2
    exit 1
fi

rundir=$1
policyfile=${rundir}/all.policy

cat >${policyfile} <<EOL
grant codeBase "file:${rundir}" { // for rmi
  permission java.security.AllPermission;
};

grant { // for db
  permission java.security.AllPermission;
};
EOL


