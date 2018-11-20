#!/bin/sh
rundir=$PWD/build/classes
path=${rundir}:$PWD/lib/cup.jar:$PWD/lib/JLex.jar

cd build/classes && java -cp ${path} pl.edu.mimuw.cloudatlas.interpreter.Main
