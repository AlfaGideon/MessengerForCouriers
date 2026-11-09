@echo off
setlocal
echo Downloading Courierka server launcher...
set "COURIERKA_PROJECT_ROOT=%~dp0"
set "COURIERKA_BACKEND_ORIGIN=http://127.0.0.1:3000"
echo Backend origin: %COURIERKA_BACKEND_ORIGIN%
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; $p=Join-Path $env:TEMP 'StartCourierka.ps1'; try { Invoke-WebRequest -UseBasicParsing 'https://raw.githubusercontent.com/AlfaGideon/MessengerForCouriers/arena/2e6c12a7-messengerforcouriers/tools/StartCourierka.ps1' -OutFile $p; $scriptText=[System.IO.File]::ReadAllText($p,[System.Text.Encoding]::UTF8); & ([scriptblock]::Create($scriptText)) } catch { Write-Host $_ -ForegroundColor Red; exit 1 }"
if errorlevel 1 (
  echo.
  echo Setup did not finish. Read the error above and try again.
  pause >nul
  exit /b 1
)
echo.
echo Launcher finished.
pause >nul
endlocal
