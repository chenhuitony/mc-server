@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul

echo Building mc-server ...

set FILES=
for /r src\main\java %%f in (*.java) do set FILES=!FILES! "%%f"
javac -d out -encoding UTF-8 %FILES%

if errorlevel 1 (
    echo Build failed
    exit /b 1
)

echo Build complete
echo Output dir: out