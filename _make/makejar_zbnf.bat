echo off
REM generating a jar file which contains all Main-classes of the ZBNF-component
REM and all classes from Zbnf and vishiaBase which were able to use in generation scripts. 
REM It contains all depending classes from the component vishiaBase. 
REM Therefore no additonal jar is necessary to run, only the java base system of any JRE (rt.jar)
REM Note that the srcJava_vishiaBase component should be present in the correct version in parallel path of this component. 
REM examples of ZBNF are not included.

REM set additional destination for zbnf.jar
REM don't use drive letters. The drive is the current one. But start on root.
REM copy only if this is the vishia/ZBNF directory or a parallel directory
set COPYJAR_CHECK=..\..\..\..\vishia\Java\Download
set JAVA_DST=\vishia\Java\vishiajar
set DST_Download=\vishia\Java\Download\exe

REM The TMP_JAVAC is a directory, which contains only this compiling results. It will be clean in the batch processing.
set TMP_JAVAC=..\..\..\tmp_javac

REM Output dir and jar-file with path and filename relative from current dir:
REM The output dir is exe usually but zbnfjax if this file is compiled in the ZBNF download preparation.
set OUTDIR_JAVAC=..\..\exe
REM regard environment zbnfjax:
if exist ..\..\..\zbnfjax\ set OUTDIR_JAVAC=..\..\..\zbnfjax
set JAR_JAVAC=zbnf.jar


REM Manifest-file for jar building relativ path from current dir:
set MANIFEST_JAVAC=zbnf.manifest

REM Input for javac, only choice of primary sources, relativ path from current (make)-directory:
set INPUT_JAVAC=
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/bridgeC/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/byteData/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/byteData/reflection_Jc/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/cmd/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/event/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/states/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/math/*.java
::set INPUT_JAVAC=%INPUT_JAVAC% ../vishia/stateMachine/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/fileLocalAccessor/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/fileRemote/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/mainCmd/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/msgDispatch/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/util/*.java
::set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/xml/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/xmlReader/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/xmlSimple/*.java

set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/byteData/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/checkDeps_C/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/docuGen/*.java
::set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/stateMGen/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/header2Reflection/*.java
::set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/sclConversions/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/zbnf/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/zbnf/ebnfConvert/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/zcmd/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/jztxtcmd/*.java


REM Sets the CLASSPATH variable for compilation (used jar-libraries). do not left empty also it aren't needed:
set CLASSPATH_JAVAC=nothing

REM Sets the src-path for further necessary sources:
set SRCPATH_JAVAC=..;../../srcJava_Zbnf

call ..\..\srcJava_Zbnf\_make\+javacjarbase.bat

@echo on
echo if this is the ZBNF folder then copy the result to some dst folder
if exist %COPYJAR_CHECK% (
  if exist %JAVA_DST% copy %OUTDIR_JAVAC%\zbnf.jar %JAVA_DST%\*
  if exist %DST_Download% copy %OUTDIR_JAVAC%\%JAR_JAVAC% %DST_Download%\%JAR_JAVAC% 
)

pause

