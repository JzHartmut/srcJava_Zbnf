echo off
REM generating a jar file which contains all Main-classes of the ZBNF-component
REM and all classes from Zbnf and vishiaBase which were able to use in generation scripts. 
REM It contains all depending classes from the component vishiaBase. 
REM Therefore no additonal jar is necessary to run, only the java base system of any JRE (rt.jar)
REM Note that the srcJava_vishiaBase component should be present in the correct version in parallel path of this component. 
REM examples of ZBNF are not included.

REM set additional destination for zbnf.jar
set FCMD_DST=..\..\..\Fcmd\sf\Fcmd\exe
set ZBNFJAX_DST=..\..\..\ZBNF\sf\ZBNF\zbnfjax

REM The TMP_JAVAC is a directory, which contains only this compiling results. It will be clean in the batch processing.
set TMP_JAVAC=..\..\..\tmp_javac

REM Output dir and jar-file with path and filename relative from current dir:
REM The output dir is exe usually but zbnfjax if this file is compiled in the ZBNF download preparation.
set OUTDIR_JAVAC=..\..\exe
REM regard environment zbnfjax:
if exist ..\..\zbnfjax\ set OUTDIR_JAVAC=..\..\zbnfjax
set JAR_JAVAC=zbnf.jar


REM Manifest-file for jar building relativ path from current dir:
set MANIFEST_JAVAC=zbnf.manifest

REM Input for javac, only choice of primary sources, relativ path from current (make)-directory:
set INPUT_JAVAC=
set INPUT_JAVAC=%INPUT_JAVAC% ../../srcJava_vishiaBase/org/vishia/bridgeC/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../../srcJava_vishiaBase/org/vishia/byteData/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../../srcJava_vishiaBase/org/vishia/byteData/reflection_Jc/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../../srcJava_vishiaBase/org/vishia/cmd/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../../srcJava_vishiaBase/org/vishia/event/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../../srcJava_vishiaBase/org/vishia/states/*.java
::set INPUT_JAVAC=%INPUT_JAVAC% ../../srcJava_vishiaBase/org/vishia/stateMachine/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../../srcJava_vishiaBase/org/vishia/fileLocalAccessor/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../../srcJava_vishiaBase/org/vishia/fileRemote/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../../srcJava_vishiaBase/org/vishia/mainCmd/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../../srcJava_vishiaBase/org/vishia/msgDispatch/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../../srcJava_vishiaBase/org/vishia/util/*.java
::set INPUT_JAVAC=%INPUT_JAVAC% ../../srcJava_vishiaBase/org/vishia/xml/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../../srcJava_vishiaBase/org/vishia/xmlSimple/*.java

set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/byteData/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/checkDeps_C/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/stateMGen/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/header2Reflection/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/sclConversions/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/zbnf/*.java
set INPUT_JAVAC=%INPUT_JAVAC% ../org/vishia/zcmd/*.java


REM Sets the CLASSPATH variable for compilation (used jar-libraries). do not left empty also it aren't needed:
set CLASSPATH_JAVAC=nothing

REM Sets the src-path for further necessary sources:
set SRCPATH_JAVAC=..;../../srcJava_vishiaBase

call ..\..\srcJava_vishiaBase\_make\+javacjarbase.bat

if exist %ZBNFJAX_DST% copy ..\..\exe\zbnf.jar %ZBNFJAX_DST%\zbnf.jar
if exist %FCMD_DST% copy ..\..\exe\zbnf.jar %FCMD_DST%\zbnf.jar

