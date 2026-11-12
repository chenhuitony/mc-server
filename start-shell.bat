@echo off
chcp 65001 >nul
cd /d "%~dp0"

if not exist build\mc-server.jar (
    echo [ERROR] build\mc-server.jar not found, run build.bat first
    exit /b 1
)

echo Shell mode: download and run the official Minecraft server
echo.

java "-Dfile.encoding=UTF-8" "-Dstdout.encoding=UTF-8" -jar build\mc-server.jar --dir run --shell vanilla %*
pause