#ECHO off
REM CLASSPATH
SET TMP_ROOT=..
SET TMP_CLASSPATH=%TMP_ROOT%
SET TMP_CLASSPATH=%TMP_CLASSPATH%;%TMP_ROOT%\build\classes
SET TMP_CLASSPATH=%TMP_CLASSPATH%;%TMP_ROOT%\lib\martj.jar
SET TMP_CLASSPATH=%TMP_CLASSPATH%;%TMP_ROOT%\lib\mysql-connector-java-3.0.7-stable-bin.jar
SET TMP_CLASSPATH=%TMP_CLASSPATH%;%TMP_ROOT%\lib\java-getopt-1.0.9.jar
SET TMP_CLASSPATH=%TMP_CLASSPATH%;%TMP_ROOT%\lib\jdom.jar
SET TMP_CLASSPATH=%TMP_CLASSPATH%;%TMP_ROOT%\lib\libreadline-java.jar
SET TMP_CLASSPATH=%TMP_CLASSPATH%;%TMP_ROOT%\lib\ensj-util.jar
SET TMP_CLASSPATH=%TMP_CLASSPATH%;%TMP_ROOT%\lib\ecp1_0beta.jar
SET TMP_CLASSPATH=%TMP_CLASSPATH%;%TMP_ROOT%\lib\jdbc2_0-stdext.jar
SET TMP_CLASSPATH=%TMP_CLASSPATH%;%TMP_ROOT%\lib\p6spy.jar
SET TMP_CLASSPATH=%TMP_CLASSPATH%;%TMP_ROOT%\lib\ojdbc14.jar

REM BUILD UP A COMMAND WITH ANY ARGUMENTS PASSED
SET com=org.ensembl.mart.lib.config.DatasetViewUploader

REM ECHO %com%

:ARGUMENT
SHIFT
SET test=%0
IF "%0" == "" GOTO PROCESS
SET com=%com% %test%
GOTO ARGUMENT

:PROCESS
java -cp %TMP_CLASSPATH% %com%
