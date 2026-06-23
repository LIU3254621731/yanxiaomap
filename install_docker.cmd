@echo off
REM Docker Desktop 安装脚本 - Windows 批处理版本
REM 需要管理员权限

echo ========================================
echo    Docker Desktop 安装脚本
echo ========================================
echo.

REM 检查 PowerShell 版本
powershell -Command "$PSVersionTable.PSVersion.Major" >nul 2>&1
if errorlevel 1 (
    echo 错误: PowerShell 未安装或版本太低
    pause
    exit /b 1
)

REM 检查是否以管理员身份运行
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo 需要管理员权限运行此脚本
    echo.
    echo 正在请求管理员权限...
    
    REM 使用 PowerShell 提升权限重新运行脚本
    powershell -Command "Start-Process cmd -ArgumentList '/c \"%~f0\"' -Verb RunAs"
    exit /b
)

echo ✓ 管理员权限确认
echo.

REM 运行 PowerShell 安装脚本
echo 正在运行 Docker 安装脚本...
echo.

powershell -ExecutionPolicy Bypass -NoProfile -File "%~dp0install_docker.ps1" -Quiet

echo.
echo ========================================
echo    安装完成
echo ========================================
echo.
echo 请按任意键退出...
pause >nul