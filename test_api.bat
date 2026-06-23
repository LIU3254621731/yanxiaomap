@echo off
chcp 65001 >nul
echo ========================================
echo   研校地图后端API测试脚本
echo ========================================
echo.

REM 检查curl是否可用
where curl >nul 2>&1
if errorlevel 1 (
    echo [错误] curl未安装
    echo 请安装curl或使用其他HTTP客户端工具
    echo 下载地址: https://curl.se/windows/
    pause
    exit /b 1
)

set BASE_URL=http://localhost:8080
set TIMEOUT=10

echo 步骤1: 检查后端服务是否运行
echo.
curl --silent --max-time %TIMEOUT% "%BASE_URL%/actuator/health" >nul 2>&1
if errorlevel 1 (
    echo [错误] 后端服务未启动或无法访问
    echo 请确保后端服务正在运行在 %BASE_URL%
    echo.
    echo 启动方法:
    echo 1. Docker方式: docker-compose up --build -d
    echo 2. Maven方式: .\mvnw.cmd spring-boot:run
    echo.
    pause
    exit /b 1
)

echo [成功] 后端服务正常运行
echo.

echo 步骤2: 测试健康检查接口
echo.
curl --silent --max-time %TIMEOUT% "%BASE_URL%/actuator/health"
echo.
echo.

echo 步骤3: 测试地图配置接口
echo.
curl --silent --max-time %TIMEOUT% "%BASE_URL%/api/map/config"
echo.
echo.

echo 步骤4: 测试院校地图数据接口（北京市985院校）
echo.
curl --silent --max-time %TIMEOUT% "%BASE_URL%/api/map/schools?province=北京市&level=985"
echo.
echo.

echo 步骤5: 测试院校详情接口（ID=1）
echo.
curl --silent --max-time %TIMEOUT% "%BASE_URL%/api/schools/1"
echo.
echo.

echo ========================================
echo   API基本功能测试完成
echo ========================================
echo.
echo 测试结果说明:
echo 1. 如果看到JSON响应，说明接口正常工作
echo 2. 如果看到错误信息，请检查:
echo    - 后端服务日志
echo    - 数据库连接状态
echo    - 网络连接
echo.
echo 详细测试指南请参考: api_test_guide.md
echo.
pause