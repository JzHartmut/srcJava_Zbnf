echo off
REM Task: Compilation javac and call jar

if "%INPUT_JAVAC%" =="" (
  echo == javacjar.sh ==
  echo script to organize javac compilation and build the jar.
  echo The environment variables should be export as Input for this script:
  echo ---------------------------------------------------------------------------------------------
  echo JAVA_JDK: Directory where bin/javac is found. This java version will taken for compilation
  echo   if not set, it searches the JDK in some directories and set it so proper as well.
  echo TMP_JAVAC: Directory for all class files and logs: The directory will be cleaned and created
  echo INPUT_JAVAC: All primary input java files to compile separated with space
  echo CLASSPATH_JAVAC: PATH where compiled classes are found, relativ from current dir or absolute
  echo SRCPATH_JAVAC: PATH where sources are found, relativ from current dir or absolute
  echo OUTDIR_JAVAC: Path where the generated jar file will be stored. It may be a relative path.
  echo   It will be created if not found.
  echo JAR_JAVAC: file.jar
  echo MANIFEST_JAVAC: Path/file.manifest relative from currect dir for manifest file

  exit 1
)

REM --------------------------------------------------------------------------------------------
REM  Environment variables set from zbnfjax:

REM  The java-copiler may be located at a user-specified position.
REM  Set the environment variable JAVA_HOME, where bin/javac will be found.
if "%JAVA_JDK%" == "" (
  if exist d:\Progs\JAVA\jdk1.6.0_21 set JAVA_JDK=D:\Progs\JAVA\jdk1.6.0_21
)
echo JAVA_JDK=%JAVA_JDK%


if not exist %OUTDIR_JAVAC% mkdir %OUTDIR_JAVAC%

REM  Delete and create the tmp_javac new. It should be empty because all content will be stored in the jar-file.
if exist %TMP_JAVAC% rmdir /S/Q %TMP_JAVAC%
mkdir %TMP_JAVAC%\bin

REM  Delete the result jar-file
del /F/Q %OUTDIR_JAVAC%\%JAR_JAVAC%
del /F/Q %OUTDIR_JAVAC%\%JAR_JAVAC%.compile.log
del /F/Q %OUTDIR_JAVAC%\%JAR_JAVAC%.compile_error.log

echo === javac -sourcepath %SRCPATH_JAVAC -classpath %CLASSPATH_JAVAC %INPUT_JAVAC

%JAVA_JDK%\bin\javac -deprecation -d %TMP_JAVAC%\bin -sourcepath %SRCPATH_JAVAC% -classpath %CLASSPATH_JAVAC% %INPUT_JAVAC% 1>>%TMP_JAVAC%\javac_ok.txt 2>%TMP_JAVAC%\javac_error.txt
if ERRORLEVEL 1 (
  echo ===Compiler error
  type %TMP_JAVAC%\javac_ok.txt 
	type %TMP_JAVAC%\javac_error.txt
  type %TMP_JAVAC%\javac_ok.txt >%OUTDIR_JAVAC%\%JAR_JAVAC%.compile_error.log
  type %TMP_JAVAC%\javac_error.txt >%OUTDIR_JAVAC%\%JAR_JAVAC%.compile_error.log
  echo see %OUTDIR_JAVAC%\%JAR_JAVAC%.compile_error.log
	call edit.bat %OUTDIR_JAVAC%\%JAR_JAVAC%.compile_error.log
	exit /B 1
)

REM  The jar works only correct, if the current directory contains the classfile tree:
REM  Store the actual directory to submit restoring on end
set ENTRYDIR=%CD%
cd %TMP_JAVAC%\bin
echo === SUCCESS compiling, generate jar: %ENTRYDIR%\%OUTDIR_JAVAC%\%JAR_JAVAC%

echo off

%JAVA_JDK%\bin\jar -cvfm %ENTRYDIR%\%OUTDIR_JAVAC%\%JAR_JAVAC% %ENTRYDIR%\%MANIFEST_JAVAC% *  1>..\jar_ok.txt 2>..\jar_error.txt

type ..\jar_error.txt >%ENTRYDIR%\%OUTDIR_JAVAC%\%JAR_JAVAC%.compile.log
type ..\jar_ok.txt >%ENTRYDIR%\%OUTDIR_JAVAC%\%JAR_JAVAC%.compile.log

if errorlevel 1 (
  echo === ERROR jar
  echo see %OUTDIR_JAVAC%\%JAR_JAVAC%.compile_error.log
	call edit.bat %OUTDIR_JAVAC%\%JAR_JAVAC%.compile_error.log
  cd %ENTRYDIR%
  exit /B 1
)

REM  Restore current dir: %ENTRYDIR%
cd %ENTRYDIR%
echo === SUCCESS making %JAR_JAVAC% in %OUTDIR_JAVAC%.

if "%NOPAUSE%" == "" pause
