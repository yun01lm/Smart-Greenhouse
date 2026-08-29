# Smart-Greenhouse 一键启动脚本（由 start_all.bat 调用）
# 后台启动 后端/Web/模拟器，日志落盘 logs\，进程与窗口生命周期解耦
#
# 新电脑使用说明：
#   1. 安装 Git / Docker Desktop / JDK17+ / Maven / Node.js / Python3（加入 PATH）
#   2. git clone 本项目到任意目录
#   3. （可选）复制 .env.example 为 .env.local 填入真实 AI Key
#   4. 双击 start_all.bat 即可
# 本脚本自动探测 mvn/npm/python 路径（PATH 优先，其次常见安装位置，可用环境变量覆盖）
$ErrorActionPreference = 'Continue'

# 项目根目录：脚本所在目录（支持任意克隆位置，不再硬编码）
$root = $PSScriptRoot
$logDir = Join-Path $root 'logs'
if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir -Force | Out-Null }

# ===== 工具路径自动探测 =====
# 优先级：环境变量 > 常见安装位置 > PATH（排除 .ps1 存根与 WindowsApps 商店存根）
function Find-ToolPath([string]$name, [string]$envVar, [string[]]$candidates) {
    $envVal = [Environment]::GetEnvironmentVariable($envVar)
    if ($envVal -and (Test-Path $envVal)) { return $envVal }
    foreach ($c in $candidates) {
        $resolved = Get-ChildItem $c -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($resolved) { return $resolved.FullName }
    }
    foreach ($cmd in @(Get-Command $name -ErrorAction SilentlyContinue)) {
        $src = $cmd.Source
        if (-not $src -or -not (Test-Path $src)) { continue }
        if ($src -like '*WindowsApps*') { continue }
        if ($src -like '*.ps1') { continue }
        return $src
    }
    return $null
}

$maven = Find-ToolPath 'mvn' 'MAVEN_HOME' @(
    'C:\apache-maven-*\bin\mvn.cmd', 'D:\apache-maven-*\bin\mvn.cmd', 'F:\apache-maven-*\bin\mvn.cmd',
    'F:\apache-maven-*\apache-maven-*\bin\mvn.cmd', 'C:\apache-maven-*\apache-maven-*\bin\mvn.cmd',
    'D:\apache-maven-*\apache-maven-*\bin\mvn.cmd',
    "$env:USERPROFILE\apache-maven-*\bin\mvn.cmd", 'C:\Program Files\apache-maven-*\bin\mvn.cmd'
)
$npm   = Find-ToolPath 'npm' 'NODE_HOME' @(
    "$env:ProgramFiles\nodejs\npm.cmd", 'C:\Program Files\nodejs\npm.cmd',
    'D:\node_js\npm.cmd', 'F:\node_js\npm.cmd', 'C:\nodejs\npm.cmd'
)
$python = Find-ToolPath 'python' 'PYTHON_HOME' @(
    "$env:LOCALAPPDATA\Programs\Python\Python*\python.exe", 'C:\Python*\python.exe',
    'D:\Python*\python.exe', 'E:\Python*\python.exe', 'E:\mingw\*\bin\python.exe', 'C:\Program Files\Python*\python.exe'
)

$webDir = Join-Path $root 'web'
$simDir = Join-Path $root 'simulator'
$url = 'http://localhost:3000'

function Write-Step([string]$msg) { Write-Host $msg -ForegroundColor Cyan }
function Write-Ok([string]$msg)   { Write-Host $msg -ForegroundColor Green }
function Write-Err([string]$msg)  { Write-Host $msg -ForegroundColor Red }

function Test-Port([int]$port) {
    try {
        $c = [System.Net.Sockets.TcpClient]::new()
        $iar = $c.BeginConnect('127.0.0.1', $port, $null, $null)
        if ($iar.AsyncWaitHandle.WaitOne(800)) { $c.EndConnect($iar); $c.Close(); return $true }
        $c.Close(); return $false
    } catch { return $false }
}

function Get-LivePid([string]$pidFile) {
    if (-not (Test-Path $pidFile)) { return $null }
    $pidVal = 0
    try { $pidVal = [int]((Get-Content $pidFile -Raw).Trim()) } catch { return $null }
    if ($pidVal -le 0) { return $null }
    try { $p = [System.Diagnostics.Process]::GetProcessById($pidVal); if (-not $p.HasExited) { return $pidVal } } catch { }
    return $null
}

# fileName=cmd.exe 时 cmdArgs 为 cmd /c 参数；直接启动程序时 cmdArgs 为程序参数
function Start-Svc([string]$fileName, [string]$cmdArgs, [string]$workDir, [hashtable]$envMap, [string]$pidFile) {
    $existing = Get-LivePid $pidFile
    if ($existing) { Write-Ok "      [OK] 已在运行 (PID $existing)，跳过"; return $existing }
    $psi = [System.Diagnostics.ProcessStartInfo]::new()
    $psi.FileName = $fileName
    $psi.Arguments = $cmdArgs
    $psi.WorkingDirectory = $workDir
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    foreach ($k in $envMap.Keys) { if (-not $psi.Environment.ContainsKey($k)) { $psi.Environment[$k] = $envMap[$k] } }
    $p = [System.Diagnostics.Process]::new()
    $p.StartInfo = $psi
    if ($p.Start()) {
        [System.IO.File]::WriteAllText($pidFile, [string]$p.Id, [System.Text.UTF8Encoding]::new($false))
        return $p.Id
    }
    return $null
}

Write-Step '================================================'
Write-Step '       智慧大棚 AIoT 系统 - 一键启动'
Write-Step '================================================'
Write-Ok "  项目目录: $root"

# 0. 加载 .env.local（Key 注入，gitignored）
$envVars = @{}
if (Test-Path (Join-Path $root '.env.local')) {
    Get-Content (Join-Path $root '.env.local') -Encoding UTF8 | ForEach-Object {
        if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$') {
            $k = $Matches[1].Trim()
            $v = if ($null -ne $Matches[2]) { $Matches[2].Trim().Trim('"') } else { '' }
            if (-not $envVars.ContainsKey($k)) { $envVars[$k] = $v }
        }
    }
    Write-Ok '[0/7] .env.local 已加载（AI Key 注入）'
} else {
    Write-Err '[0/7] 未找到 .env.local，AI 保持 Mock 模式（复制 .env.example 可启用真实 AI）'
}

# 1. 环境检查
Write-Step '[1/7] 环境检查 ...'
$missing = @()
foreach ($pair in @(@('Maven', $maven), @('npm', $npm), @('Python', $python))) {
    if (-not $pair[1]) { $missing += $pair[0] } else { Write-Ok "      [OK] $($pair[0]): $($pair[1])" }
}
if ($missing.Count -gt 0) {
    Write-Err "[!!] 未找到: $($missing -join ', ')"
    Write-Err '     请安装并加入 PATH，或设置环境变量 MAVEN_HOME / NODE_HOME / PYTHON_HOME'
    Read-Host '按回车退出'
    exit 1
}

# 2. Docker
Write-Step '[2/7] 检查 Docker ...'
docker info *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Err '[!!] Docker 未运行，请先启动 Docker Desktop 再运行本脚本'
    Read-Host '按回车退出'
    exit 1
}
Write-Ok '      [OK] Docker 运行中'

# 3. 中间件容器
Write-Step '[3/7] 检查中间件容器 ...'
$mysqlUp = docker ps --format '{{.Names}}' | Where-Object { $_ -eq 'greenhouse-mysql' }
if (-not $mysqlUp) {
    Write-Step '      [..] 容器未启动，执行 docker compose up -d ...'
    Push-Location $root
    docker compose up -d
    Pop-Location
    Write-Step '      等待 20 秒 ...'
    Start-Sleep -Seconds 20
} else {
    Write-Ok '      [OK] 中间件容器运行中'
}

# 4. 后端
Write-Step '[4/7] 启动后端 (8080) ...'
$backendLog = Join-Path $logDir 'backend.log'
if (Test-Port 8080) {
    Write-Ok '      [OK] 后端已在 8080 运行，跳过'
} else {
    $bid = Start-Svc 'cmd.exe' ('/c ' + $maven + ' spring-boot:run -pl backend > ' + $backendLog + ' 2>&1') $root $envVars (Join-Path $logDir 'backend.pid')
    if ($bid) { Write-Step "      已启动 (PID $bid)，日志: $backendLog" } else { Write-Err '[!!] 后端启动失败'; exit 1 }
}

# 5. 等待后端就绪（ddl-auto 自动建表完成后才能灌种子数据）
Write-Step '[等待] 后端就绪 (最长 300 秒) ...'
$waited = 0
while ($waited -lt 300 -and -not (Test-Port 8080)) { Start-Sleep -Seconds 3; $waited += 3 }
if (Test-Port 8080) { Write-Ok '      [OK] 后端就绪 (8080)' } else { Write-Err '[!!] 后端超时未就绪，请查看 logs\backend.log' }

# 6. 数据库自动初始化（新电脑空库自动灌种子数据 + 固件建档，幂等）
Write-Step '[5/7] 数据库初始化检查 ...'
$mysqlReady = $false
for ($i = 0; $i -lt 30; $i++) {
    docker exec greenhouse-mysql mysql -uroot -proot -e "SELECT 1" *> $null
    if ($LASTEXITCODE -eq 0) { $mysqlReady = $true; break }
    Start-Sleep -Seconds 2
}
if (-not $mysqlReady) {
    Write-Err '      [!!] MySQL 未就绪，跳过初始化（后端启动时会自动建表，可稍后手动执行 tools\init_seed_data.sql）'
} else {
    $devCount = (docker exec greenhouse-mysql mysql -uroot -proot -N -e "SELECT COUNT(*) FROM smart_greenhouse.devices" 2>$null).Trim()
    if ($devCount -match '^\d+$' -and [int]$devCount -gt 0) {
        Write-Ok "      [OK] 数据库已有数据（devices=$devCount），跳过初始化"
    } else {
        Write-Step '      [..] 空库，执行种子数据 + 固件迁移 ...'
        Get-Content (Join-Path $root 'tools\init_seed_data.sql') -Raw | docker exec -i greenhouse-mysql mysql -uroot -proot smart_greenhouse 2>&1 | Out-Null
        Get-Content (Join-Path $root 'tools\seed_data.sql') -Raw | docker exec -i greenhouse-mysql mysql -uroot -proot smart_greenhouse 2>&1 | Out-Null
        Get-Content (Join-Path $root 'tools\migrate_firmware_id.sql') -Raw | docker exec -i greenhouse-mysql mysql -uroot -proot smart_greenhouse 2>&1 | Out-Null
        $final = (docker exec greenhouse-mysql mysql -uroot -proot -N -e "SELECT COUNT(*) FROM smart_greenhouse.devices" 2>$null).Trim()
        Write-Ok "      [OK] 初始化完成（devices=$final）"
    }
}

# 7. Web
Write-Step '[6/7] 启动 Web (3000) ...'
$webLog = Join-Path $logDir 'web.log'
if (Test-Port 3000) {
    Write-Ok '      [OK] Web 已在 3000 运行，跳过'
} else {
    $wid = Start-Svc 'cmd.exe' ('/c cd /d ' + $webDir + ' && call ' + $npm + ' run dev > ' + $webLog + ' 2>&1') $webDir @{} (Join-Path $logDir 'web.pid')
    if ($wid) { Write-Step "      已启动 (PID $wid)，日志: $webLog" } else { Write-Err '[!!] Web 启动失败'; exit 1 }
}

# 8. 模拟器（cmd 无引号形式，日志落盘）
Write-Step '[7/7] 启动设备模拟器 ...'
$simLog = Join-Path $logDir 'simulator.log'
$sid = Start-Svc 'cmd.exe' ('/c ' + $python + ' -u device_simulator.py --mode normal --config devices.json > ' + $simLog + ' 2>&1') $simDir @{} (Join-Path $logDir 'simulator.pid')
if ($sid) { Write-Step "      已启动 (PID $sid)，日志: $simLog" } else { Write-Err '[!!] 模拟器启动失败'; exit 1 }

# 9. 等待 Web 就绪
Write-Step '[等待] Web 就绪 (最长 60 秒) ...'
$waited = 0
while ($waited -lt 60 -and -not (Test-Port 3000)) { Start-Sleep -Seconds 2; $waited += 2 }
if (Test-Port 3000) {
    Write-Ok '      [OK] Web 就绪 (3000)'
    try {
        $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 5
        Write-Ok "      [OK] 页面 HTTP $($r.StatusCode)"
    } catch { Write-Err '      [..] 页面响应异常，稍后刷新重试' }
} else { Write-Err '[!!] Web 超时未就绪，请查看 logs\web.log' }

Write-Step '================================================'
Write-Ok '  全部服务已启动'
Write-Step "  访问: $url"
Write-Step '  账号: admin / owner01 / expert01 / worker01 （密码 123456）'
Write-Step "  日志目录: $logDir"
Write-Step '  停止服务: 运行 stop_all.bat（Docker 容器保留）'
Write-Step '================================================'
Start-Process $url
