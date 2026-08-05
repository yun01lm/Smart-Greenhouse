# Smart-Greenhouse 一键启动脚本（由 start_all.bat 调用）
# 后台启动 后端/Web/模拟器，日志落盘 logs\，进程与窗口生命周期解耦
$ErrorActionPreference = 'Continue'
$root = 'F:\Smart_project\Smart-Greenhouse'
$logDir = Join-Path $root 'logs'
if (-not (Test-Path $logDir)) { New-Item -ItemType Directory -Path $logDir -Force | Out-Null }
$maven = 'F:\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd'
$npm   = 'F:\node_js\npm.cmd'
$python = 'E:\mingw\ucrt64\bin\python.exe'
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
    Write-Ok '[0/6] .env.local 已加载（AI Key 注入）'
} else {
    Write-Err '[0/6] 未找到 .env.local，AI 保持 Mock 模式'
}

# 1. 环境检查
Write-Step '[1/6] 环境检查 ...'
foreach ($pair in @(@('Maven', $maven), @('npm', $npm), @('Python', $python))) {
    if (-not (Test-Path $pair[1])) { Write-Err "[!!] 未找到 $($pair[0]): $($pair[1])"; exit 1 }
}
Write-Ok '      [OK] Maven / npm / Python 就绪'

# 2. Docker
Write-Step '[2/6] 检查 Docker ...'
docker info *> $null
if ($LASTEXITCODE -ne 0) {
    Write-Err '[!!] Docker 未运行，请先启动 Docker Desktop 再运行本脚本'
    Read-Host '按回车退出'
    exit 1
}
Write-Ok '      [OK] Docker 运行中'

# 3. 中间件容器
Write-Step '[3/6] 检查中间件容器 ...'
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
Write-Step '[4/6] 启动后端 (8080) ...'
$backendLog = Join-Path $logDir 'backend.log'
if (Test-Port 8080) {
    Write-Ok '      [OK] 后端已在 8080 运行，跳过'
} else {
    $bid = Start-Svc 'cmd.exe' ('/c ' + $maven + ' spring-boot:run -pl backend > ' + $backendLog + ' 2>&1') $root $envVars (Join-Path $logDir 'backend.pid')
    if ($bid) { Write-Step "      已启动 (PID $bid)，日志: $backendLog" } else { Write-Err '[!!] 后端启动失败'; exit 1 }
}

# 5. Web
Write-Step '[5/6] 启动 Web (3000) ...'
$webLog = Join-Path $logDir 'web.log'
if (Test-Port 3000) {
    Write-Ok '      [OK] Web 已在 3000 运行，跳过'
} else {
    $wid = Start-Svc 'cmd.exe' ('/c cd /d ' + $webDir + ' && call ' + $npm + ' run dev > ' + $webLog + ' 2>&1') $webDir @{} (Join-Path $logDir 'web.pid')
    if ($wid) { Write-Step "      已启动 (PID $wid)，日志: $webLog" } else { Write-Err '[!!] Web 启动失败'; exit 1 }
}

# 6. 模拟器（cmd 无引号形式，日志落盘）
Write-Step '[6/6] 启动设备模拟器 ...'
$simLog = Join-Path $logDir 'simulator.log'
$sid = Start-Svc 'cmd.exe' ('/c ' + $python + ' -u device_simulator.py --mode normal --config devices.json > ' + $simLog + ' 2>&1') $simDir @{} (Join-Path $logDir 'simulator.pid')
if ($sid) { Write-Step "      已启动 (PID $sid)，日志: $simLog" } else { Write-Err '[!!] 模拟器启动失败'; exit 1 }

# 7. 等待就绪
Write-Step '[等待] 后端就绪 (最长 300 秒) ...'
$waited = 0
while ($waited -lt 300 -and -not (Test-Port 8080)) { Start-Sleep -Seconds 3; $waited += 3 }
if (Test-Port 8080) { Write-Ok '      [OK] 后端就绪 (8080)' } else { Write-Err '[!!] 后端超时未就绪，请查看 logs\backend.log' }

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
