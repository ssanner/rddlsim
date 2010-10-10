@echo off
set HOME=..
set LIB_HOME=%HOME%\lib
set LIBJARS=..
for %%i in (%LIB_HOME%\*.jar) do call %HOME%\bin\cpappend.bat %%i
if "%LIBJARS%" == "" goto staticClassPath
goto gotLibJars
:staticClassPath
if exist "%LIB_HOME%\jtp.jar" set LIBJARS=%LIB_HOME%\jtp.jar
if exist "%LIB_HOME%\antlr.jar" set LIBJARS=%LIBJARS%;%LIB_HOME%\antlr.jar
if exist "%LIB_HOME%\xerces.jar" set LIBJARS=%LIBJARS%;%LIB_HOME%\xerces.jar
if exist "%LIB_HOME%\jena.jar" set LIBJARS=%LIBJARS%;%LIB_HOME%\jena.jar
if exist "%LIB_HOME%\jdom.jar" set LIBJARS=%LIBJARS%;%LIB_HOME%\jdom.jar
if exist "%LIB_HOME%\jakarta-oro-2.0.5.jar" set LIBJARS=%LIBJARS%;%LIB_HOME%\jakarta-oro-2.0.5.jar
:gotLibJars
echo %LIBJARS%
java -classpath %LIBJARS% %1 %2 %3 %4 %5
