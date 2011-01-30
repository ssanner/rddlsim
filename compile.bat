@@echo off
rem Note: Need to run from JavaRelated directory.
set LOCAL_HOME=.
set LIB_HOME=%LOCAL_HOME%\lib
set LIBJARS=%LOCAL_HOME%\src
set BINDIR=%LOCAL_HOME%\bin
for %%i in (%LIB_HOME%\*.jar) do call %LOCAL_HOME%\cpappend.bat %%i
mkdir %BINDIR%
javac -classpath %LIBJARS% -d %BINDIR% %LOCAL_HOME%\src\rddl\*.java %LOCAL_HOME%\src\rddl\competition\*.java %LOCAL_HOME%\src\rddl\parser\*.java %LOCAL_HOME%\src\rddl\policy\*.java %LOCAL_HOME%\src\rddl\translate\*.java %LOCAL_HOME%\src\rddl\validate\*.java %LOCAL_HOME%\src\rddl\viz\*.java
