@echo off
setlocal EnableExtensions

:: Relaunch elevated; Docker Desktop uninstall normally needs Administrator rights.
fltmc >nul 2>&1
if errorlevel 1 (
  echo Requesting Administrator permission...
  powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "Start-Process -FilePath '%~f0' -Verb RunAs"
  exit /b
)

title Remove Docker Desktop
echo.
echo This will uninstall Docker Desktop. The official EXE uninstaller is called
echo with --keep-data to preserve Docker's VM data where supported.
echo Do not use this if you need Docker Desktop to keep running.
echo.
choice /C YN /M "Continue with Docker Desktop uninstall"
if errorlevel 2 exit /b 0

:: Stop Docker processes and service before calling the official uninstaller.
taskkill /IM "Docker Desktop.exe" /T /F >nul 2>&1
taskkill /IM "com.docker.backend.exe" /T /F >nul 2>&1
taskkill /IM "com.docker.proxy.exe" /T /F >nul 2>&1
sc.exe stop com.docker.service >nul 2>&1

echo.
echo Looking for the official Docker Desktop uninstaller...
set "UNINSTALLER=%ProgramFiles%\Docker\Docker\Docker Desktop Installer.exe"
if exist "%UNINSTALLER%" goto uninstall_exe
set "UNINSTALLER=%LOCALAPPDATA%\Programs\DockerDesktop\Docker Desktop Installer.exe"
if exist "%UNINSTALLER%" goto uninstall_exe

echo Official EXE uninstaller was not found. Trying winget if available...
where winget.exe >nul 2>&1
if not errorlevel 1 (
  echo.
  echo WARNING: this fallback uses the registered Windows uninstaller.
  echo Unlike the EXE path, it may remove Docker images, containers, and volumes.
  choice /C YN /M "Try the winget uninstall fallback"
  if errorlevel 2 (
    echo Uninstall cancelled. No Docker data folders were manually deleted.
    pause
    exit /b 0
  )
  winget.exe uninstall --id Docker.DockerDesktop --exact --interactive
  if not errorlevel 1 goto success
)

echo.
echo Could not find a usable uninstaller. Opening Windows Installed Apps.
start "" ms-settings:appsfeatures
echo Find Docker Desktop in the list. If this still fails, send the exact error text.
pause
exit /b 1

:uninstall_exe
echo Found: "%UNINSTALLER%"
echo Running the official uninstaller and asking it to keep Docker data...
"%UNINSTALLER%" uninstall --keep-data
if errorlevel 1 (
  echo.
  echo The official uninstaller returned an error. No Docker data folders were deleted.
  echo If you have an MSI installation, the MSI installer/product code may be required.
  pause
  exit /b 1
)

:success
echo.
echo Docker Desktop uninstall command completed.
echo Restart Windows before installing Docker Desktop again.
echo This script does not manually delete Docker images, volumes, WSL data, or settings.
echo.
pause
exit /b 0
