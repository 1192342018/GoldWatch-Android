@echo off
setlocal
set GRADLE_VERSION=8.9
set APP_HOME=%~dp0
set BOOT_DIR=%APP_HOME%.gradle-bootstrap
set GRADLE_HOME=%BOOT_DIR%\gradle-%GRADLE_VERSION%
set ZIP=%BOOT_DIR%\gradle-%GRADLE_VERSION%-bin.zip
if not exist "%GRADLE_HOME%\bin\gradle.bat" (
  if not exist "%BOOT_DIR%" mkdir "%BOOT_DIR%"
  if not exist "%ZIP%" (
    echo Downloading Gradle %GRADLE_VERSION%...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing -Uri 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile '%ZIP%'"
    if errorlevel 1 exit /b 1
  )
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Force -Path '%ZIP%' -DestinationPath '%BOOT_DIR%'"
  if errorlevel 1 exit /b 1
)
call "%GRADLE_HOME%\bin\gradle.bat" %*
endlocal
