@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"

echo Building mc-server.jar ...

if exist out rmdir /s /q out
if exist build rmdir /s /q build
mkdir out
mkdir build

dir /s /b src\main\java\*.java > build\sources.txt
javac -d out -encoding UTF-8 @build\sources.txt
if errorlevel 1 (
    echo [ERROR] Compilation failed
    exit /b 1
)
del /q build\sources.txt

echo Main-Class: com.mc.Main> build\MANIFEST.MF
echo Implementation-Title: mc-server>> build\MANIFEST.MF
echo Implementation-Version: 1.0.0>> build\MANIFEST.MF
echo Implementation-Vendor: chenhuitony>> build\MANIFEST.MF

jar --create --file build\mc-server.jar --manifest build\MANIFEST.MF -C out .
if errorlevel 1 (
    echo [ERROR] Packaging failed
    exit /b 1
)

del /q build\MANIFEST.MF

echo.
echo [OK] build\mc-server.jar
dir /b build