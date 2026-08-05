# Smart-Greenhouse 一键停止脚本
# 停止 后端8080 / Web3000 / 设备模拟器 进程；Docker 容器保留运行
$ErrorActionPreference = 'SilentlyContinue'
Write-Host '正在停止 Smart-Greenhouse 服务（后端 8080 / Web 3000 / 设备模拟器）...' -ForegroundColor Cyan
$pids = @()
$pids += (netstat -ano | Select-String ':8080\s.*LISTENING').Line -replace '.*\s(\d+)\s*$', '$1'
$pids += (netstat -ano | Select-String ':3000\s.*LISTENING').Line -replace '.*\s(\d+)\s*$', '$1'
$pids | Where-Object { $_ -match '^\d+$' } | Select-Object -Unique | ForEach-Object {
    Stop-Process -Id ([int]$_) -Force -ErrorAction SilentlyContinue
    Write-Host "  [OK] 已停止 PID $_" -ForegroundColor Green
}
Get-CimInstance Win32_Process | Where-Object { $_.Name -eq 'python.exe' -and $_.CommandLine -like '*device_simulator*' } | ForEach-Object {
    Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
    Write-Host "  [OK] 已停止模拟器 PID $($_.ProcessId)" -ForegroundColor Green
}
Get-ChildItem 'F:\Smart_project\Smart-Greenhouse\logs\*.pid' -ErrorAction SilentlyContinue | Remove-Item -Force
Write-Host '完成：Docker 容器保留运行。' -ForegroundColor Green
Start-Sleep -Seconds 2
