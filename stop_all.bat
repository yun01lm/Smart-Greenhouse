@echo off
chcp 936 >nul
title Smart-Greenhouse Ò»¼üÍ£Ö¹
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop_all.ps1"
echo.
pause
