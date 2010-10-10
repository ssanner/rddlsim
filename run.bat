@@echo off
rem Note: Need to run from JavaRelated directory.
set LOCAL_HOME=.
set LIB_HOME=%LOCAL_HOME%\lib
set LIBJARS=%LOCAL_HOME%\bin
for %%i in (%LIB_HOME%\*.jar) do call %LOCAL_HOME%\cpappend.bat %%i
java -Xms200M -Xmx1024M -classpath %LIBJARS% %1 %2 %3 %4 %5 %6 %7 %8 %9

