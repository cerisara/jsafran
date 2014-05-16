JSafran et Jtrans are both copyrighted to Christophe Cerisara.

======================
LICENCE

Cecill-C licence (similar to L-GPL)

======================
WHAT IS IT ALL ABOUT ?

JSafran & JTrans constitute an integrated Java software for speech
corpus processing, edition and transformation, from:
1- 1/2-automatic Speech to text alignment
2- Automatic speech transcriptions
3- Automatic POS-tagging
4- Automatic dependency parsing
5- French automatic semantic role labelling (in progress)

It allows for editing, browsing, transforming with rules, and 
controlling these five processings via GUIs speech and text data.


======================
HOW TO USE IT ? ++++++ TUTORIAL

Please follow the following steps to make sure everything is ok:

1- unzip jsafran.tgz in an empty directory
2- cd into that directory
3- launch jsafran:
	./jsafran.sh
   The JSafran window shall appear
4- load the raw text file "culture.txt":
	Menu File / Load
   The text shall appear in the main GUI.
5- tag the text:
	5.1 press lower-case "z" to show the current POS-tags: they should all be equal to "unk"
	5.2 Menu Parsing / retag all
   After a few seconds, the real (estimated) POS tags shall replace the "unk" default tags.
6- parse the text:
	Menu Parsing / "parse all w/ Malt"
   After a few seconds, you should see the dependency arcs proposed by the Malt parser on top of the utterances.
   (note that you use the broadcast news models that have been trained without any punctuation mark; so, don't be
   surprised if you see many dependency errors around punctuations !)
7- edit the automatic parsing:
	go to utterance 4: "le président déchu et la gauche ..."
      select the fourth word "et"
	press "d" to toggle edit mode: you should see a new loop-dependency in red
      move the head of this new dependency arc up to the verb "dénoncent" on the right
	press "d" again to quit the edit mode
	you have fixed an annotation error !
8- align the text with the audio file
	Menu File / listen to Audio
	You will be asked whether you want to use a previous alignement, or create a new one.
	As you don't have any alignement yet, we will create a new one. So, choose "NO".
	The JTRANS window will pop-up with the text already loaded in it.
	8.a. analyze the text to know what is pronunced and what is not: JTrans / menu edit / parse text standard
		After a few seconds, you see segments in the text highlighted in different colors.
	8.b. load the corresponding wav file: JTrans / menu file / load wav
		Select the file "culture.wav"
		You should see the spectrogram in the lower part of the JTrans window
	8.c. ask JTrans to automatically align the text and audio file: JTrans / button "AutoAlign"
		You have to wait for a few minutes while all the acoustic and language models loads up (you need at least
		1Gbyte of RAM for this to succeeds)
		You shall then see the alignement progress in the text itself, as every word is underlined in blue as soon as it is
		aligned.
      8.d. after the words have started to be underlined in blue, you can check the alignment by clicking on the button "play":
		you should then hear the wav file along with a kind a "karaoke" playback mode.
	8.e. save the alignment: JTrans / menu File / save project : give a file name like cult.jtr
9- you can now come back to the JSafran window, and navigate to any utterance.
   The next time you select "listen to audio" in JSafran's menu, the JTrans window will automatically position itself
   at the correct place, so that when you click on "play", you here the audio corresponding to the line selected in JSafran.

======================
CORPUS DESCRIPTION

test2009.xml est composé de fichiers de France Inter de 1999

train2011.xml a été enregistré en 3 sessions:

- session1 (logerot, melynda, ...: 2009): fichiers de FranceInter de 1999 et de 2002 (TRAIN de ESTER2)
- session2 (corinna, melynda, ...: 2009-2010): fichiers de FranceInter de 1999 et de 2002 (TRAIN de ESTER2)
- session3 (melynda: été 2010): fichiers de FranceInter de 2007 (DEV de ESTER2)

