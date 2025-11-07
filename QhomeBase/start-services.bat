@echo off
echo ========================================
echo Starting QhomeBase Services for Flutter App
echo ========================================
echo.

REM Get the directory where this script is located
set SCRIPT_DIR=%~dp0
cd /d "%SCRIPT_DIR%"

echo [1/3] Starting IAM Service (port 8088)...
start "IAM Service" cmd /k "cd /d "%SCRIPT_DIR%iam-service" && mvn spring-boot:run -DskipTests"
timeout /t 5 /nobreak > nul

echo [2/3] Starting Base Service (port 8081)...
start "Base Service" cmd /k "cd /d "%SCRIPT_DIR%base-service" && mvn spring-boot:run -DskipTests"
timeout /t 5 /nobreak > nul

echo [3/3] Starting Finance Billing Service (port 8085)...
start "Finance Billing Service" cmd /k "cd /d "%SCRIPT_DIR%finance-billing-service" && mvn spring-boot:run -DskipTests"
timeout /t 5 /nobreak > nul

echo.
echo ========================================
echo All services are starting...
echo ========================================
echo.
echo Services:
echo   - IAM Service:        http://localhost:8088
echo   - Base Service:       http://localhost:8081
echo   - Finance Billing:    http://localhost:8085
echo.
echo Please wait for services to fully start (check logs in each window)
echo.
pause

