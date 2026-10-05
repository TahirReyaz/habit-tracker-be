@REM ----------------------------------------------------------------------------
@REM Maven Wrapper for Windows (cmd and PowerShell). Self-contained.
@REM First run downloads Maven into %USERPROFILE%\.m2\wrapper\dists, then runs it.
@REM Version: .mvn\wrapper\maven-wrapper.properties if present, else the default
@REM in the PowerShell section at the bottom of this file.
@REM
@REM   cmd:         mvnw spring-boot:run
@REM   PowerShell:  .\mvnw spring-boot:run
@REM ----------------------------------------------------------------------------
@echo off
setlocal
set "MVNW_BASEDIR=%~dp0"
if "%MVNW_BASEDIR:~-1%"=="\" set "MVNW_BASEDIR=%MVNW_BASEDIR:~0,-1%"
set "MVNW_SELF=%~f0"

set "MAVEN_HOME="
for /f "usebackq delims=" %%M in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "$s = Get-Content -LiteralPath $env:MVNW_SELF -Raw; $m = '#' + 'POWERSHELL' + '#'; Invoke-Expression ($s.Substring($s.IndexOf($m) + $m.Length))"`) do set "MAVEN_HOME=%%M"

if not defined MAVEN_HOME (
  echo mvnw: could not install Maven, see the error above 1>&2
  exit /b 1
)
if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  echo mvnw: "%MAVEN_HOME%" is not a Maven installation 1>&2
  exit /b 1
)

set "MAVEN_PROJECTBASEDIR=%MVNW_BASEDIR%"
cd /d "%MVNW_BASEDIR%"
call "%MAVEN_HOME%\bin\mvn.cmd" %*
exit /b %ERRORLEVEL%

#POWERSHELL#
# Everything below runs in PowerShell (cmd never reaches it). Prints MAVEN_HOME on stdout.
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
$defaultUrl = 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip'

$url = $defaultUrl
$props = Join-Path $env:MVNW_BASEDIR '.mvn\wrapper\maven-wrapper.properties'
if (Test-Path -LiteralPath $props) {
  $line = Get-Content -LiteralPath $props | Where-Object { $_ -match '^\s*distributionUrl\s*=' } | Select-Object -Last 1
  if ($line) { $url = ($line -replace '^\s*distributionUrl\s*=\s*', '').Trim() }
}
if ($env:MVNW_REPOURL) {
  $url = $env:MVNW_REPOURL.TrimEnd('/') + '/org/apache/maven/' + ($url -replace '^.*?/org/apache/maven/', '')
}

$file = $url.Substring($url.LastIndexOf('/') + 1)
$name = ($file -replace '\.zip$', '') -replace '-bin$', ''
$sha  = [System.Security.Cryptography.SHA256]::Create()
$hash = (($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($url)) | ForEach-Object { $_.ToString('x2') }) -join '').Substring(0, 16)
$userHome  = if ($env:MAVEN_USER_HOME) { $env:MAVEN_USER_HOME } else { Join-Path $env:USERPROFILE '.m2' }
$dist      = Join-Path $userHome "wrapper\dists\$name\$hash"
$mavenHome = Join-Path $dist $name

if (-not (Test-Path -LiteralPath (Join-Path $mavenHome 'bin\mvn.cmd'))) {
  New-Item -ItemType Directory -Force -Path $dist | Out-Null
  $tmp = Join-Path $dist ('tmp' + [Guid]::NewGuid().ToString('N'))
  New-Item -ItemType Directory -Force -Path $tmp | Out-Null
  try {
    [Console]::Error.WriteLine("Downloading $url")
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    $wc = New-Object Net.WebClient
    if ($env:MVNW_USERNAME) { $wc.Credentials = New-Object Net.NetworkCredential($env:MVNW_USERNAME, $env:MVNW_PASSWORD) }
    $zip = Join-Path $tmp $file
    $wc.DownloadFile($url, $zip)
    Expand-Archive -LiteralPath $zip -DestinationPath $tmp -Force
    if (-not (Test-Path -LiteralPath (Join-Path $tmp $name))) { throw "archive did not contain $name" }
    if (Test-Path -LiteralPath $mavenHome) { Remove-Item -Recurse -Force -LiteralPath $mavenHome }
    Move-Item -LiteralPath (Join-Path $tmp $name) -Destination $mavenHome
  } finally {
    Remove-Item -Recurse -Force -LiteralPath $tmp -ErrorAction SilentlyContinue
  }
}
Write-Output $mavenHome
