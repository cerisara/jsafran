#!/bin/tcsh

sed 's,#xxcd ,cd ,g' jsafran.sh > ../jsafran.sh 
cp README.txt ../

cd ..
chmod 755 jsafran.sh

tar zcvf jtools.tgz jtrans/res jtrans/ressources jsafran/tagger jsafran/jsynats.cfg jsafran/svmmods.mco culture.wav culture.txt culture.jtr jsafran.sh README.txt jtrans/dist/jtrans.jar jsafran/lib/anna.jar jsafran/dist/jsafran.jar jsafran/lib/malt.jar jsafran/lib/log4j.jar jsafran/lib/libsvm.jar jsafran/lib/org.annolab.tt4j-1.0.12.jar jsafran/lib/opennlp-tools-1.4.3.jar jsafran/lib/maxent-2.5.2.jar jsafran/lib/trove.jar jsafran/lib/liblinear-1.33-with-deps.jar jsafran/lib/detcrf.jar jtrans/libs/sphinx4.jar jtrans/libs/basicplayer3.0.jar jtrans/libs/commons-logging-api.jar jtrans/libs/JHTK.jar jtrans/libs/jl1.0.jar jtrans/libs/jogg-0.0.7.jar jtrans/libs/jorbis-0.0.15.jar jtrans/libs/jspeex0.9.7.jar jtrans/libs/JUNG.jar jtrans/libs/junit-4.5.jar jtrans/libs/mp3spi1.9.4.jar jtrans/libs/tritonus_gsm-0.3.6.jar jtrans/libs/tritonus_remaining.jar jtrans/libs/tritonus_share.jar jtrans/libs/vorbisspi1.0.2.jar jtrans/libs/weka.jar

