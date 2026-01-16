@echo off
echo Generating ANTLR parser for DSL

cd /d "%~dp0"
if not exist "antlr_generated" mkdir antlr_generated

where java >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Java not found!
    echo Please install Java to run ANTLR
    exit /b 1
)

set ANTLR_JAR=antlr-4.13.0-complete.jar
set ANTLR_URL=https://www.antlr.org/download/antlr-4.13.0-complete.jar

if not exist "%ANTLR_JAR%" (
    echo ANTLR JAR file not found. Attempting to download
    echo Downloading %ANTLR_JAR% from %ANTLR_URL%
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%ANTLR_URL%' -OutFile '%ANTLR_JAR%'"
    if not exist "%ANTLR_JAR%" (
        echo.
        echo ERROR: Failed to download ANTLR JAR file
        echo Please manually download from: %ANTLR_URL%
        echo And place it in: %CD%
        exit /b 1
    )
    echo Download successful!
)

if exist "%ANTLR_JAR%" (
    echo Using Java with ANTLR 4.13.0 JAR file
    java -jar "%ANTLR_JAR%" -Dlanguage=Python3 -o antlr_generated -visitor -listener DSL.g4
    if %ERRORLEVEL% EQU 0 goto success
    echo.
    echo ERROR: Failed to generate ANTLR parser
    exit /b 1
)

where antlr4 >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo Using antlr4 command (may be older version)
    antlr4 -Dlanguage=Python3 -o antlr_generated -visitor -listener DSL.g4
    if %ERRORLEVEL% EQU 0 goto success
    echo.
    echo ERROR: Failed to generate ANTLR parser
    exit /b 1
)

echo ERROR: Neither ANTLR JAR file nor antlr4 command found!
exit /b 1

:success
echo.
echo SUCCESS: ANTLR parser generated successfully!
echo Generated files are in: antlr_generated/
