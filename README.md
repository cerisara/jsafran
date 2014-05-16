JSafran
=======

Syntactic parser for French (and English).

Main features:

* Easy GUI for edition of dependency trees
* Multi-levels: chunks, dep. trees, semantic graphs...
* Include the MATE and Malt parsers, and the Treetagger for automatic training and parsing
* Regular-expression based query/edition language
* Beta code for unsupervised dependency parsing

How to compile
--------------

Just run

    ant jar

All dependencies should be already take into account.
To run jsafran:

    java -jar jsafran.jar

Please add an issue if you encounter problems.

Dependencies and licence
------------------------
Please look at the doc/dependencies_and_licence.md file that details all libraries used by JSafran.
