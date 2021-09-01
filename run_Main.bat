echo %JAVA_HOME%
echo off
call delete_logs
call setenv_1.8.0_73
set CLASSPATH=%CLASSPATH%;.\deploy\jbones_schedule-config.jar;.\deploy\jbones_schedule.jar;
echo using classpath ...
echo %CLASSPATH%

"%JAVA_HOME%\bin\java" -classpath %CLASSPATH% org.jbones.schedule.Main

pause
