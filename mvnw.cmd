@echo off
setlocal
set BASEDIR=%~dp0
set WRAPPER_JAR=%BASEDIR%.mvn\wrapper\maven-wrapper.jar
if not exist "%WRAPPER_JAR%" (
  echo Baixando maven-wrapper.jar ...
  powershell -Command "(New-Object Net.WebClient).DownloadFile('https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar', '%WRAPPER_JAR%')"
)
java -classpath "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
