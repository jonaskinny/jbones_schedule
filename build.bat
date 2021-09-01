echo off
set JAVA_HOME=
set PATH=
set CLASSPATH=

call setenv_1.8.0_73

echo using CLASSPATH %_LIBJARS%
echo using JAVA_HOME %JAVA_HOME%

"%JAVA_HOME%\bin\java" -classpath %_LIBJARS% org.apache.tools.ant.Main %1

echo [JBONES_PROJECT_BUILT]

pause




