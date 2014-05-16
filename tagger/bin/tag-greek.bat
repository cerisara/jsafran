@echo off

set TAGDIR=C:\TreeTagger

set BIN=%TAGDIR%\bin
set CMD=%TAGDIR%\cmd
set LIB=%TAGDIR%\lib
set TAGOPT=%LIB%\greek.par -token -lemma -sgml -no-unknown

if "%2"=="" goto label1
perl %CMD%\tokenize.pl "%~1" | perl %CMD%\mwl-lookup-greek.perl |
%BIN%\tree-tagger %TAGOPT% > "%~2"
goto end

:label1
if "%1"=="" goto label2
perl %CMD%\tokenize.pl "%~1" | perl %CMD%\mwl-lookup-greek.perl |
%BIN%\tree-tagger %TAGOPT%
goto end

:label2
echo.
echo Usage: tag-greek file {file}
echo.

:end

