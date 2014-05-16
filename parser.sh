#!/bin/csh

set JCP = "lib/liblinear-1.8.jar:lib/trove.jar:../jsafran/jsafran.jar:lib/maxent-2.5.2.jar:../utils/bin:build:bin"
set mem = 7g

setenv PATH $PATH":../../tools"

set TRAIN = ../../corpus/brown/train.conll
set TRAIN = ../jsafran/trainFTB.xml
set TRAIN = ../jsafran/trainFTBshort.xml
set TRAIN = ../jsafran/train2011.xml
set TRAIN = ../jsafran/trainFTB.xml

set TEST = ../../corpus/brown/test.conll
# set TEST = ../jsafran/pb.xml
set TEST = ../jsafran/test2009.xml
set TEST = ../jsafran/testFTB.xml

rm -f log
touch log
java -Xmx$mem -cp "$JCP" jsafran.parsing.MEMMparser -libtrain $TRAIN
foreach rk (1 2 3 4)
  java -Xmx$mem -cp "$JCP" jsafran.parsing.MEMMparser -gen $rk
  foreach p (0.01 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.9 1)
    set las=`java -Xmx$mem -cp "$JCP" jsafran.parsing.MEMMparser -rank $rk -notrain -hmm tmp.def$rk -test $TEST -naprior $p | grep LAS | cut -d'=' -f2`
    echo $p" "$las >> log
  end
  echo "" >> log
end
exit

echo "generate"
# java -Xmx$mem -cp "$JCP" jsafran.parsing.MEMMparser -gen $rk
# java -Xmx$mem -cp "$JCP" jsafran.parsing.MEMMparser -rank $rk -hmm tmp.def$rk -libtrain train2011.xml
# echo "verifie que le HMM genere n'a pas de soucis a parser les graphes non-projectifs"
# java -Xmx$mem -cp "$JCP" jsafran.parsing.MEMMparser -rank $rk -hmm tmp.def$rk -notrain -forcealign unproj.xml
echo "evalue sur le test"
java -Xmx$mem -cp "$JCP" jsafran.parsing.MEMMparser -rank $rk -hmm tmp.def$rk -notrain -naprior 0.5 -test test2009.xml

# dot -v -Tps -o tt.ps hmm4.dot
exit
echo "check HMM3"
java -Xmx$mem -cp "$JCP" jsafran.parsing.MEMMparser -rank 3 -hmm hmm.def3 -notrain -forcealign test2009.xml
exit


# goto tmp
java -Xmx$mem -cp "$JCP" jsafran.parsing.MEMMparser -libtrain train2011.xml
# java -Xmx$mem -cp "$JCP" jsafran.parsing.MEMMparser -libtest test2009.xml

prio:
rm -f log
touch log
foreach p (0.01 0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.9 1)
  echo $p >> log
  java -Xmx$mem -cp "$JCP" jsafran.parsing.MEMMparser -rank $rk -notrain -hmm tmp.def$rk -test test2009.xml -naprior $p | grep LAS >> log
end

# java -Xmx$mem -cp "$JCP" jsafran.parsing.MEMMparser -savefeats train2011.xml
# mv feats.out feats_train.out
# java -Xmx$mem -cp "$JCP" jsafran.parsing.MEMMparser -savefeats test2009.xml
# mv feats.out feats_test.out
# java -Xmx$mem -cp "$JCP" jsafran.MateParser -test test.xml
exit

echo "debut"
date
echo "training"
java -Xmx$mem -cp "$JCP" jsafran.parsing.MEMMparser -hmm hmm.def1 -rank 1 -train $TRAIN -test $TEST -malt mateETBtrain.xml $*
exit

echo "testing"
java -Xmx$mem -cp "$JCP" jsafran.parsing.MEMMparser -hmm hmm.def1 -rank 1 -notrain -test $TEST -malt mateETBtest.xml $*
echo "completing with MATE"
java -Xmx$mem -cp "$JCP" jsafran.MateParser -test res.xml

# java -Xmx$mem -jar ../jsafran/jsafran.jar -toconll res.xml

# java -jar ~/softs/malt-1.6.1/malt.jar -c maltm.mods -m learn -i $TRAIN:r".conll" -l liblinear
# java -jar ~/softs/malt-1.6.1/malt.jar -c maltm.mods -m parse -i $TEST:r".conll" -l liblinear -o output.conll
# echo "EVAL SUR DEPS DE LEN=1"
# java -Xmx$mem -cp "$JCP" jsafran.parsing.NodesSeq -calcacc 1 output.conll $TEST

./evalconll.pl -q -g $TEST:r".conll" -s output.conll

echo "fin"
date
exit

cd ../jsafran
./jsafran.sh -toconll ../EN/rec.xml
mv -f output.conll rec.conll
./jsafran.sh -toconll traintest.xml
# ./jsafran.sh -toconll test2009.xml
mv -f output.conll ref.conll
./evalconll.pl -q -g ref.conll -s rec.conll

# java -cp "$JCP" -ea parsing.DetParser -n 3 -train ../jsafran/train2011.xml -parse ../jsafran/traintest.xml
# java -cp "$JCP" -ea parsing.DetParser -parse ../jsafran/test2009.xml

# java -cp "$JCP" -ea parsing.DetParser -analyse 2 -analyseTarget LA1SUJ -parse ../jsafran/traintest.xml
# java -cp "$JCP" -ea parsing.DetParser -eval out.xml ../jsafran/traintest.xml

exit

cp out.xml ../jsafran

tmp:
cd ../jsafran
./jsafran.sh -toconll out.xml
mv output.conll rec.conll
./jsafran.sh -toconll test2009.xml
mv output.conll ref.conll
./evalconll.pl -q -g ref.conll -s rec.conll

