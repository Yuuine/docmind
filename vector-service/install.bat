@echo off
chcp 65001 >nul 2>&1
setlocal EnableDelayedExpansion

echo ========================================
echo Vector Service Dependency Installer
echo ========================================
echo.

python --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Python not found. Please install Python first.
    echo.
    pause
    exit /b 1
)

for /f "tokens=2" %%i in ('python --version 2^>^&1') do set PYTHON_VERSION=%%i
echo [OK] Python version: %PYTHON_VERSION%

if not exist "venv" (
    echo.
    echo [INFO] Creating virtual environment...
    python -m venv venv
    if errorlevel 1 (
        echo [ERROR] Failed to create virtual environment
        echo.
        pause
        exit /b 1
    )
    echo [OK] Virtual environment created
) else (
    echo [OK] Virtual environment already exists
)

echo.
echo [INFO] Activating virtual environment...
call venv\Scripts\activate.bat

if errorlevel 1 (
    echo [ERROR] Failed to activate virtual environment
    echo.
    pause
    exit /b 1
)

echo [OK] Virtual environment activated

echo.
echo [INFO] Upgrading pip...
python -m pip install --upgrade pip >nul 2>&1
if errorlevel 1 (
    echo [WARNING] Failed to upgrade pip, continuing anyway...
)

echo.
echo ========================================
echo [INFO] Installing dependencies...
echo ========================================

pip install -r requirements.txt

if errorlevel 1 (
    echo.
    echo [ERROR] Failed to install dependencies
    echo.
    pause
    exit /b 1
)

echo.
echo ========================================
echo [SUCCESS] Installation completed!
echo ========================================
echo.
echo To start the service:
echo.
echo   1. Activate environment:
echo      call venv\Scripts\activate.bat
echo.
echo   2. Start service:
echo      uvicorn main:app --host 0.0.0.0 --port 8001 --reload
echo.
echo Or run directly:
echo   .\venv\Scripts\python -m uvicorn main:app --host 0.0.0.0 --port 8001 --reload
echo.
echo.
pause
