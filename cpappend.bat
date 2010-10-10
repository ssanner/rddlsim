if "%LIBJARS%" == "" goto setInitial
set LIBJARS=%LIBJARS%;%1
goto finish
:setInitial
set LIBJARS=%1
:finish
