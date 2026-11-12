@echo off
chcp 65001 >nul

echo 启动 mc-server ...
echo.

cmd /c "java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp out com.mc.Main --dir run"