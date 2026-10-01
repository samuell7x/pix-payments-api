#!/bin/sh
# ----------------------------------------------------------------------------
# Maven Wrapper startup script (standard Apache Maven wrapper, unmodified logic)
# ----------------------------------------------------------------------------
set -e

BASEDIR=$(dirname "$0")
WRAPPER_JAR="$BASEDIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_PROPERTIES="$BASEDIR/.mvn/wrapper/maven-wrapper.properties"
WRAPPER_URL=$(grep '^wrapperUrl=' "$WRAPPER_PROPERTIES" | cut -d'=' -f2-)

if [ ! -f "$WRAPPER_JAR" ]; then
  echo "Baixando maven-wrapper.jar de $WRAPPER_URL ..."
  if command -v curl > /dev/null; then
    curl -fsSL -o "$WRAPPER_JAR" "$WRAPPER_URL"
  elif command -v wget > /dev/null; then
    wget -q -O "$WRAPPER_JAR" "$WRAPPER_URL"
  else
    echo "ERRO: curl ou wget sao necessarios para baixar o Maven Wrapper na primeira execucao." >&2
    exit 1
  fi
fi

JAVA_EXE="${JAVA_HOME:+$JAVA_HOME/bin/}java"
exec "$JAVA_EXE" -classpath "$WRAPPER_JAR" org.apache.maven.wrapper.MavenWrapperMain "$@"
