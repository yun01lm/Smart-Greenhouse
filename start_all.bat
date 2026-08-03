@echo off
setlocal enabledelayedexpansion
chcp 936 >nul
title Smart-Greenhouse ????

set "ROOT=F:\Smart_project\Smart-Greenhouse"
set "MAVEN=F:\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd"
set "NPM=F:\node_js\npm.cmd"
set "PYTHON=E:\mingw\ucrt64\bin\python.exe"
set "URL=http://localhost:3000"

echo ================================================
echo        ????AIoT?? - ????
echo ================================================
echo.

REM ========== 1. ?????? ==========
echo [1/6] ?????? ...
if not exist "%MAVEN%" (
    echo   [??] ??? Maven: %MAVEN%
    echo   ???????? MAVEN ??
    pause
    exit /b 1
)
if not exist "%NPM%" (
    echo   [??] ??? npm: %NPM%
    echo   ???????? NPM ??
    pause
    exit /b 1
)
if not exist "%PYTHON%" (
    echo   [??] ??? Python: %PYTHON%
    echo   ???????? PYTHON ??
    pause
    exit /b 1
)
echo   [OK] ??????

REM ========== 2. Docker ?? ==========
echo [2/6] ?? Docker ...
docker info >nul 2>&1
if %errorlevel% neq 0 (
    echo   [??] Docker ???????? Docker Desktop ???????
    pause
    exit /b 1
)
echo   [OK] Docker ???

REM ========== 3. ?????? ==========
echo [3/6] ???????? ...
docker ps --format "{{.Names}}" | findstr /c:"greenhouse-mysql" >nul 2>&1
if %errorlevel% neq 0 (
    echo   [??] ????????? docker compose up -d ...
    cd /d "%ROOT%"
    docker compose up -d
    echo   ???????20 ??...
    timeout /t 20 /nobreak >nul
) else (
    echo   [OK] ?????
)

REM ========== 4. ???? ==========
echo [4/6] ?????? ...
netstat -ano | findstr /c:":8080 " | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 (
    echo   [OK] ?????? ??8080
) else (
    echo   [??] ???? Spring Boot ??8080 ...
    start "Greenhouse-Backend" cmd /k "cd /d %ROOT% && call %MAVEN% spring-boot:run -pl backend"
)

REM ========== 5. Web ?? ==========
echo [5/6] ?? Web ?? ...
netstat -ano | findstr /c:":3000 " | findstr "LISTENING" >nul 2>&1
if %errorlevel% equ 0 (
    echo   [OK] Web ?????? ??3000
) else (
    if not exist "%ROOT%\web\node_modules" (
        echo   [??] ??????????? npm install ...
        start "Greenhouse-Web-Install" cmd /k "cd /d %ROOT%\web && call %NPM% install"
        echo   ??????????? 120 ??...
        timeout /t 120 /nobreak >nul
    )
    echo   [??] ???? Web ?? Vite ??3000 ...
    start "Greenhouse-Web" cmd /k "cd /d %ROOT%\web && call %NPM% run dev"
)

REM ========== 6. ????? ==========
echo [6/6] ??????? ...
powershell -NoProfile -Command "if ((Get-CimInstance Win32_Process -Filter \"name='python.exe'\" | Where-Object { $_.CommandLine -like '*device_simulator*' } | Measure-Object).Count -gt 0) { exit 0 } else { exit 1 }" >nul 2>&1
set "SIM_RUNNING=%errorlevel%"
if %SIM_RUNNING% equ 0 (
    echo   [OK] ?????????
) else (
    echo   [??] ????????? MQTT ...
    start "Greenhouse-Simulator" cmd /k "cd /d %ROOT%\simulator && %PYTHON% device_simulator.py --mode normal --config devices.json"
)

REM ========== 7. ?????? ==========
echo.
echo ?????? ...
set /a waited=0
:wait_backend
timeout /t 3 /nobreak >nul
netstat -ano | findstr /c:":8080 " | findstr "LISTENING" >nul 2>&1
if %errorlevel% neq 0 (
    set /a waited+=3
    if !waited! lss 150 goto wait_backend
    echo   [??] ?? 150 ??????????????????
    goto done
)
echo   [OK] ???? ??8080

echo ?? Web ?? ...
set /a waited=0
:wait_web
timeout /t 2 /nobreak >nul
netstat -ano | findstr /c:":3000 " | findstr "LISTENING" >nul 2>&1
if %errorlevel% neq 0 (
    set /a waited+=2
    if !waited! lss 60 goto wait_web
    echo   [??] ?? 60 ?? Web ???????? Web ????
    goto done
)
echo   [OK] Web ?? ??3000

:done
echo.
echo ================================================
echo   ?????
echo   ????: %URL%
echo   ????: owner01 / 123456
echo   ????: ?????????????
echo ================================================
start "" "%URL%"
timeout /t 10 /nobreak >nul
exit /b 0
