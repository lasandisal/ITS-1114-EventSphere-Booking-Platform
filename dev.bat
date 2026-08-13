@echo off
setlocal EnableExtensions

REM =============================================================================
REM EventSphere Backend Development Server Startup Script (dev.bat)
REM Windows development server with clean process lifecycle management.
REM =============================================================================

REM 1. Read PORT from .env configuration (or default to 7080)
set "PORT=7080"

if exist ".env" (
    for /f "tokens=1,* delims==" %%A in ('findstr /b "PORT=" .env') do (
        if not "%%B"=="" set "PORT=%%B"
    )
)

REM Remove possible quotes, apostrophes, and spaces
set "PORT=%PORT:"=%"
set "PORT=%PORT:'=%"
set "PORT=%PORT: =%"

REM 2. Verify Java is available
where java >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java was not found on PATH.
    echo Please install Java 21 and make sure java.exe is available.
    exit /b 1
)

REM 3. Print startup banner
echo.
echo ========================================
echo EventSphere backend starting...
echo Java: 21
echo Port: %PORT%
echo ========================================
echo.

REM 4. Launch Spring Boot through Maven Wrapper
call mvnw.cmd spring-boot:run

REM 5. Maven/Spring Boot has stopped.
echo.
echo Maven/Spring Boot process has exited.
echo Checking port %PORT%...
echo.

REM 6. Find processes listening on the configured port
set "FOUND_PROCESS=0"

for /f "tokens=5" %%P in ('netstat -ano ^| findstr /R /C:":%PORT% .*LISTENING"') do (
    set "FOUND_PROCESS=1"

    echo Found process %%P listening on port %PORT%.
    echo Stopping process tree...

    taskkill /PID %%P /T /F >nul 2>&1

    if errorlevel 1 (
        echo WARNING: Could not terminate process %%P.
    ) else (
        echo Process %%P terminated successfully.
    )
)

REM 7. Verify that the port is actually free
if "%FOUND_PROCESS%"=="0" (
    echo No process was listening on port %PORT%.
) else (
    echo.
    echo Verifying port %PORT% is released...

    timeout /t 1 /nobreak >nul

    netstat -ano | findstr /R /C:":%PORT% .*LISTENING" >nul 2>&1

    if errorlevel 1 (
        echo Port %PORT% released successfully.
    ) else (
        echo WARNING: Port %PORT% is still in use.
        echo Run:
        echo     netstat -ano ^| findstr :%PORT%
    )
)

echo.
echo Backend stopped.
echo.

endlocal