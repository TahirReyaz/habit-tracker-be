#!/bin/sh
# ----------------------------------------------------------------------------
# Maven Wrapper (only-script flavour)
#
# Downloads Maven into ~/.m2/wrapper/dists on first use, then runs it.
# The version comes from .mvn/wrapper/maven-wrapper.properties when that file
# exists, otherwise from DEFAULT_DISTRIBUTION_URL below. Needs Java on PATH or
# JAVA_HOME, plus curl or wget (unzip optional; falls back to Java's jar tool).
#
#   ./mvnw spring-boot:run
#   ./mvnw test
#
# Optional env: MAVEN_USER_HOME, MVNW_REPOURL (mirror), MVNW_VERBOSE=true,
#               MVNW_USERNAME / MVNW_PASSWORD (authenticated mirror)
# ----------------------------------------------------------------------------
set -eu

die() { printf 'mvnw: %s\n' "$*" >&2; exit 1; }
verbose() { if [ "${MVNW_VERBOSE-}" = true ]; then printf 'mvnw: %s\n' "$*"; fi; }

# --- locate the project base dir (the directory containing .mvn) ----------
DEFAULT_DISTRIBUTION_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip"
BASE_DIR=$(cd "$(dirname "$0")" && pwd -P)
PROPS="$BASE_DIR/.mvn/wrapper/maven-wrapper.properties"

# --- java -----------------------------------------------------------------
if [ -n "${JAVA_HOME-}" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
  [ -x "$JAVACMD" ] || JAVACMD="$JAVA_HOME/bin/java.exe"
  [ -x "$JAVACMD" ] || die "JAVA_HOME is set to '$JAVA_HOME' but no java executable was found there"
else
  JAVACMD=$(command -v java || true)
  [ -n "$JAVACMD" ] || die "java not found. Install JDK 21 and/or set JAVA_HOME."
fi

# --- read distributionUrl ---------------------------------------------------
distributionUrl=""
if [ -f "$PROPS" ]; then
  distributionUrl=$(sed -n 's/^[[:space:]]*distributionUrl[[:space:]]*=[[:space:]]*//p' "$PROPS" | tr -d '\r' | tail -n 1)
fi
[ -n "$distributionUrl" ] || distributionUrl="$DEFAULT_DISTRIBUTION_URL"
if [ -n "${MVNW_REPOURL-}" ]; then
  distributionUrl="${MVNW_REPOURL%/}/org/apache/maven/${distributionUrl#*/org/apache/maven/}"
fi

distributionFile=${distributionUrl##*/}             # apache-maven-3.9.11-bin.zip
distributionName=${distributionFile%.zip}
distributionName=${distributionName%-bin}           # apache-maven-3.9.11

# hash of the URL keeps different mirrors/versions apart, like the official wrapper
hash_url() {
  if command -v sha256sum >/dev/null 2>&1; then printf %s "$1" | sha256sum | cut -c1-16
  elif command -v shasum >/dev/null 2>&1; then printf %s "$1" | shasum -a 256 | cut -c1-16
  elif command -v md5sum >/dev/null 2>&1; then printf %s "$1" | md5sum | cut -c1-16
  else printf %s "$1" | cksum | cut -d' ' -f1
  fi
}

MAVEN_USER_HOME="${MAVEN_USER_HOME:-${HOME}/.m2}"
DIST_DIR="$MAVEN_USER_HOME/wrapper/dists/$distributionName/$(hash_url "$distributionUrl")"
MAVEN_HOME="$DIST_DIR/$distributionName"

# --- download + unpack on first run -----------------------------------------
if [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  verbose "installing $distributionName into $DIST_DIR"
  mkdir -p "$DIST_DIR"
  TMP_DIR=$(mktemp -d "$DIST_DIR/tmp.XXXXXX")
  trap 'rm -rf "$TMP_DIR"' EXIT HUP INT TERM

  printf 'Downloading %s\n' "$distributionUrl"
  if command -v curl >/dev/null 2>&1; then
    if [ -n "${MVNW_USERNAME-}" ]; then
      curl -fsSL --retry 3 -u "$MVNW_USERNAME:${MVNW_PASSWORD-}" -o "$TMP_DIR/$distributionFile" "$distributionUrl"
    else
      curl -fsSL --retry 3 -o "$TMP_DIR/$distributionFile" "$distributionUrl"
    fi
  elif command -v wget >/dev/null 2>&1; then
    if [ -n "${MVNW_USERNAME-}" ]; then
      wget -q --user="$MVNW_USERNAME" --password="${MVNW_PASSWORD-}" -O "$TMP_DIR/$distributionFile" "$distributionUrl"
    else
      wget -q -O "$TMP_DIR/$distributionFile" "$distributionUrl"
    fi
  else
    die "neither curl nor wget is available to download Maven"
  fi

  if command -v unzip >/dev/null 2>&1; then
    unzip -q "$TMP_DIR/$distributionFile" -d "$TMP_DIR"
  else
    JAR_CMD="$(dirname "$JAVACMD")/jar"
    [ -x "$JAR_CMD" ] || [ -x "$JAR_CMD.exe" ] || die "unzip not found and no 'jar' tool next to java"
    (cd "$TMP_DIR" && "$JAR_CMD" xf "$distributionFile")
  fi
  [ -d "$TMP_DIR/$distributionName" ] || die "archive did not contain $distributionName"
  chmod +x "$TMP_DIR/$distributionName/bin/mvn" 2>/dev/null || true
  rm -rf "$MAVEN_HOME"
  mv "$TMP_DIR/$distributionName" "$MAVEN_HOME"
  rm -rf "$TMP_DIR"
  trap - EXIT HUP INT TERM
fi

# --- run --------------------------------------------------------------------
export MAVEN_PROJECTBASEDIR="$BASE_DIR"
[ -z "${JAVA_HOME-}" ] || export JAVA_HOME
cd "$BASE_DIR"
exec "$MAVEN_HOME/bin/mvn" "$@"
