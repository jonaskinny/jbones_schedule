echo off
set CLASSPATH=
set DATA_ROOT=C:\Users\jonat\OneDrive\DATA
set TOOLS=%DATA_ROOT%\tools
set JAVA_HOME=%TOOLS%\jdk1.8.0_73
set PROJECT_ROOT=%DATA_ROOT%\jbones\jbones_schedule
set _LIBJARS=
for %%i in (%PROJECT_ROOT%\lib\*.*) do call cpappend.bat %%i
for %%i in (%PROJECT_ROOT%\lib\ext\*.*) do call cpappend.bat %%i
rem for %%i in (%TOOLS%\*.*) do call cpappend.bat %%i
for %%i in (%JAVA_HOME%\lib\*.*) do call cpappend.bat %%i
set CLASSPATH=%CLASSPATH%;%_LIBJARS%

