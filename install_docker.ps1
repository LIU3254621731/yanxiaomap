# Docker Desktop 安装脚本
# 适用于 Windows 10/11，使用 WSL2 后端
# 需要管理员权限

param(
    [switch]$Quiet = $false,
    [string]$InstallPath = "$env:ProgramFiles\Docker\Docker"
)

Write-Host "=== Docker Desktop 安装脚本 ===" -ForegroundColor Cyan
Write-Host "开始时间: $(Get-Date)" -ForegroundColor Gray

# 检查管理员权限
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Host "需要管理员权限，尝试提升权限..." -ForegroundColor Yellow
    $scriptPath = $MyInvocation.MyCommand.Path
    $arguments = "-ExecutionPolicy Bypass -NoProfile -File `"$scriptPath`" -Quiet:$Quiet"
    Start-Process powershell -Verb RunAs -ArgumentList $arguments -Wait
    exit $LASTEXITCODE
}

Write-Host "✓ 管理员权限确认" -ForegroundColor Green

# 检查系统要求
Write-Host "`n检查系统要求..." -ForegroundColor Cyan

# 检查 Windows 版本
$os = Get-WmiObject -Class Win32_OperatingSystem
$windowsVersion = [System.Environment]::OSVersion.Version
Write-Host "操作系统: $($os.Caption)" -ForegroundColor Gray
Write-Host "版本: $($os.Version)" -ForegroundColor Gray

if ($windowsVersion.Major -lt 10) {
    Write-Host "错误: Docker Desktop 需要 Windows 10 或更高版本" -ForegroundColor Red
    exit 1
}

# 检查架构
if ([Environment]::Is64BitOperatingSystem) {
    Write-Host "✓ 64位系统" -ForegroundColor Green
} else {
    Write-Host "错误: Docker Desktop 需要 64位系统" -ForegroundColor Red
    exit 1
}

# 检查 WSL2
try {
    $wslInfo = wsl --list --verbose 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ WSL2 已安装" -ForegroundColor Green
        Write-Host "WSL 发行版:" -ForegroundColor Gray
        $wslInfo | ForEach-Object { Write-Host "  $_" -ForegroundColor Gray }
    } else {
        Write-Host "警告: WSL2 未安装，Docker 将使用 Hyper-V 模式" -ForegroundColor Yellow
    }
} catch {
    Write-Host "警告: 无法检查 WSL2 状态" -ForegroundColor Yellow
}

# 下载 Docker Desktop 安装程序
Write-Host "`n下载 Docker Desktop 安装程序..." -ForegroundColor Cyan
$tempDir = $env:TEMP
$installerPath = Join-Path $tempDir "DockerDesktopInstaller.exe"
$downloadUrl = "https://desktop.docker.com/win/main/amd64/Docker%20Desktop%20Installer.exe"

Write-Host "下载 URL: $downloadUrl" -ForegroundColor Gray
Write-Host "保存路径: $installerPath" -ForegroundColor Gray

try {
    # 删除已存在的安装程序
    if (Test-Path $installerPath) {
        Remove-Item $installerPath -Force
    }

    # 下载安装程序
    $ProgressPreference = 'SilentlyContinue'
    Invoke-WebRequest -Uri $downloadUrl -OutFile $installerPath -UseBasicParsing
    
    if (Test-Path $installerPath) {
        $fileSize = (Get-Item $installerPath).Length / 1MB
        Write-Host "✓ 下载完成，文件大小: $($fileSize.ToString('0.00')) MB" -ForegroundColor Green
    } else {
        Write-Host "错误: 下载失败，文件不存在" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "错误: 下载失败 - $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# 安装 Docker Desktop
Write-Host "`n安装 Docker Desktop..." -ForegroundColor Cyan

$installArgs = @()
if ($Quiet) {
    $installArgs += "--quiet"
}
$installArgs += "--accept-license"
$installArgs += "--installation-dir=`"$InstallPath`""

$installCmd = "`"$installerPath`" $($installArgs -join ' ')"
Write-Host "安装命令: $installCmd" -ForegroundColor Gray

try {
    Write-Host "正在安装，请稍候..." -ForegroundColor Yellow
    $process = Start-Process -FilePath $installerPath -ArgumentList $installArgs -Wait -PassThru
    
    if ($process.ExitCode -eq 0) {
        Write-Host "✓ Docker Desktop 安装成功" -ForegroundColor Green
    } else {
        Write-Host "警告: 安装程序退出代码: $($process.ExitCode)" -ForegroundColor Yellow
        Write-Host "可能需要手动完成安装" -ForegroundColor Yellow
    }
} catch {
    Write-Host "错误: 安装失败 - $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# 验证安装
Write-Host "`n验证 Docker 安装..." -ForegroundColor Cyan

# 添加 Docker 到 PATH
$dockerPath = "$InstallPath\Docker\resources\bin"
if (Test-Path $dockerPath) {
    $env:Path += ";$dockerPath"
    [Environment]::SetEnvironmentVariable("Path", $env:Path, [EnvironmentVariableTarget]::Machine)
    Write-Host "✓ Docker 路径已添加到系统 PATH" -ForegroundColor Green
}

# 检查 Docker 版本
Start-Sleep -Seconds 5  # 等待服务启动
try {
    $dockerVersion = docker --version 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ $dockerVersion" -ForegroundColor Green
    } else {
        Write-Host "警告: Docker 命令未找到，可能需要重启或手动启动 Docker Desktop" -ForegroundColor Yellow
    }
} catch {
    Write-Host "警告: 无法检查 Docker 版本" -ForegroundColor Yellow
}

# 启动 Docker Desktop
Write-Host "`n启动 Docker Desktop..." -ForegroundColor Cyan
try {
    # 尝试通过服务启动
    Start-Service -Name "Docker Desktop Service" -ErrorAction SilentlyContinue
    
    # 或者通过命令行启动
    $dockerExe = "$InstallPath\Docker\Docker Desktop.exe"
    if (Test-Path $dockerExe) {
        Start-Process $dockerExe -ErrorAction SilentlyContinue
        Write-Host "✓ Docker Desktop 已启动" -ForegroundColor Green
    } else {
        Write-Host "信息: 请手动启动 Docker Desktop" -ForegroundColor Gray
    }
} catch {
    Write-Host "信息: 请手动启动 Docker Desktop" -ForegroundColor Gray
}

# 完成信息
Write-Host "`n=== 安装完成 ===" -ForegroundColor Cyan
Write-Host "完成时间: $(Get-Date)" -ForegroundColor Gray
Write-Host "`n下一步操作:" -ForegroundColor Yellow
Write-Host "1. 检查 Docker Desktop 是否正常运行" -ForegroundColor Gray
Write-Host "2. 运行 'docker --version' 验证安装" -ForegroundColor Gray
Write-Host "3. 运行项目中的 'docker-build-run.cmd' 启动后端服务" -ForegroundColor Gray
Write-Host "`n注意: 首次启动可能需要重启系统" -ForegroundColor Magenta

if (-not $Quiet) {
    Read-Host "按 Enter 键退出"
}