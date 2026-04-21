@echo off
SET JDK=C:\Program Files\Java\jdk1.8.0_201
SET JAVA=%JDK%\bin\java.exe

SET GROOVY=C:\Program Files\Java\groovy.sdk3.0.4
SET OSP=O:\OneDrive\Projects\Groovy\OrderStatementParser

REM %JAVA% -classpath "%OSP%\out\production\OrderStatementParser\*;%OSP%\lib\*" com.asbytes.Main

copy .\summaries\Summary.html .\summaries\Summary-last.html

SET JRELIB=%JDK%\jre\lib\charsets.jar;%JDK%\jre\lib\deploy.jar;%JDK%\jre\lib\ext\access-bridge-64.jar;%JDK%\jre\lib\ext\cldrdata.jar;%JDK%\jre\lib\ext\dnsns.jar;%JDK%\jre\lib\ext\jaccess.jar;%JDK%\jre\lib\ext\jfxrt.jar;%JDK%\jre\lib\ext\localedata.jar;%JDK%\jre\lib\ext\nashorn.jar;%JDK%\jre\lib\ext\sunec.jar;%JDK%\jre\lib\ext\sunjce_provider.jar;%JDK%\jre\lib\ext\sunmscapi.jar;%JDK%\jre\lib\ext\sunpkcs11.jar;%JDK%\jre\lib\ext\zipfs.jar;%JDK%\jre\lib\javaws.jar;%JDK%\jre\lib\jce.jar;%JDK%\jre\lib\jfr.jar;%JDK%\jre\lib\jfxswt.jar;%JDK%\jre\lib\jsse.jar;%JDK%\jre\lib\management-agent.jar;%JDK%\jre\lib\plugin.jar;%JDK%\jre\lib\resources.jar;%JDK%\jre\lib\rt.jar

"%JAVA%" -Dfile.encoding=UTF-8 -classpath "%JRELIB%;%OSP%\out\production\OrderStatementParser;%GROOVY%\lib\*;%OSP%\lib\*" com.asbytes.Main

pause

if %ERRORLEVEL% EQU 0 (
   explorer .\summaries\Summary.html
)

