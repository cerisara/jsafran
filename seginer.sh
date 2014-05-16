#!/bin/tcsh

cd /home/xtof/softs/cclparser
echo "/home/xtof/git/jsafran/train2011.word word learn -o etblearn -s log -p -R 100" >! conf
echo "/home/xtof/git/jsafran/test2009.word word parse -o etbtest -s prs -p -R 100" >> conf
./cclparser conf
