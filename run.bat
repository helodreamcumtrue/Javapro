@echo off
title Goal Decomposition Engine
echo ========================================================
echo   Goal Decomposition Engine (Spring Boot + SQLite)
echo   100%% Local, Offline, Rule-Based Project
echo ========================================================
echo.

where java >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Java is not installed or not in PATH!
    echo Please install JDK 17 or higher from https://adoptium.net/
    pause
    exit /b 1
)

where mvn >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven is not installed or not in PATH!
    echo Please install Apache Maven from https://maven.apache.org/
    pause
    exit /b 1
)

echo [1/2] Compiling and starting application...
echo Web UI will be accessible at: http://localhost:8080
echo.

start "" http://localhost:8080
call mvn spring-boot:run
pause
