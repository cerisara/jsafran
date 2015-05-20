JSafran
=======

Java GUI for editing/parsing dependency trees.

Main features:

* NEW: includes Stanford PCFG tagger/parser for English
* Easy GUI for edition of dependency trees
* Multi-levels: chunks, dep. trees, semantic graphs...
* Include the MATE and Malt parsers, and the Treetagger for automatic training and parsing
* Regular-expression based query/edition language
* Beta code for unsupervised dependency parsing with manual rules
* Beta code for collaborative edition via GIT

How to compile
--------------

Just run

    ant jar

All dependencies should be already take into account.
To run jsafran:

    java -jar jsafran.jar

It will download all resources at the first run.
You may optionally add a file name on the command-line. The file type is detected from its extension:

* xml : jsafran format; it shall typically be a file previously saved from jsafran
* txt : text file with one sentence per line
* conll09 : CoNLL'09 format

Other types (CoNLL'06, '08...) are supported from the graphical menus.

Please add an issue if you encounter problems.

Dependencies and licence
------------------------
Please look at the doc/dependencies_and_licence.md file that details all libraries used by JSafran.
