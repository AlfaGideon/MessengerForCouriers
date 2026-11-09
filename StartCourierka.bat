@echo off
setlocal
echo Downloading Courierka server launcher...
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; $p=Join-Path $env:TEMP 'StartCourierka.ps1'; try { Invoke-WebRequest -UseBasicParsing 'https://raw.githubusercontent.com/AlfaGideon/MessengerForCouriers/arena/52ee8420-messengerforcouriers/tools/StartCourierka.ps1' -OutFile $p; & $p } catch { Write-Host $_ -ForegroundColor Red; exit 1 }"
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
