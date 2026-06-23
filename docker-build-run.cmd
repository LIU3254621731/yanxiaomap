@echo off
echo ========================================
echo 研校地图项目 Docker 构建与运行脚本
echo ========================================
echo.

REM 检查Docker是否安装
docker --version >nul 2>&1
if errorlevel 1 (
    echo [错误] Docker未安装或未启动
    echo 请先安装Docker Desktop并启动Docker服务
    pause
    exit /b 1
)

REM 检查docker-compose是否可用
docker-compose version >nul 2>&1
if errorlevel 1 (
    echo [警告] docker-compose未单独安装，将使用Docker内置compose
)

echo 步骤1: 复制环境变量文件
if not exist .env (
    copy .env.example .env
    echo [提示] 已创建.env文件，请编辑该文件配置您的环境变量
    echo.
)

echo 步骤2: 构建和启动所有服务
echo 这可能需要几分钟时间...
echo.
docker-compose up --build -d

if errorlevel 1 (
    echo [错误] Docker Compose启动失败
    echo 请检查错误信息并确保端口3306, 6379, 8080未被占用
    pause
    exit /b 1
)

echo.
echo ========================================
echo 服务启动成功！
echo ========================================
echo.
echo 服务访问地址:
echo - 后端API: http://localhost:8080
echo - 数据库: localhost:3306 (用户: root, 密码: root)
echo - Redis: localhost:6379
echo.
echo API文档:
echo - Swagger UI: http://localhost:8080/swagger-ui.html
echo - API列表: http://localhost:8080/v3/api-docs
echo.
echo 常用命令:
echo - 查看日志: docker-compose logs -f
echo - 停止服务: docker-compose down
echo - 重启服务: docker-compose restart
echo - 重建服务: docker-compose up --build -d
echo.
echo 按任意键查看服务状态...
pause >nul

echo.
docker-compose ps
echo.
pause