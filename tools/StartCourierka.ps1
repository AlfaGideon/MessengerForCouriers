$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

function Write-Step([string]$Message) {
    Write-Host "`n== $Message" -ForegroundColor Cyan
}

Write-Host 'Курьерка — запуск backend и временного глобального адреса' -ForegroundColor Yellow

$dockerDesktopCli = Join-Path $env:ProgramFiles 'Docker\Docker\resources\bin\docker.exe'
$dockerCommand = Get-Command docker.exe -ErrorAction SilentlyContinue
if (Test-Path $dockerDesktopCli) {
    $dockerExe = $dockerDesktopCli
} elseif ($dockerCommand) {
    $dockerExe = $dockerCommand.Source
} else {
    Write-Host 'Сначала установите Docker Desktop: https://www.docker.com/products/docker-desktop/' -ForegroundColor Red
    Start-Process 'https://www.docker.com/products/docker-desktop/'
    throw 'Установите Docker Desktop, перезагрузите компьютер при запросе и запустите этот BAT-файл снова.'
}

Write-Step 'Проверяю Docker'
Write-Host "Использую Docker CLI: $dockerExe"
$dockerReady = $false
try { & $dockerExe info *> $null; $dockerReady = ($LASTEXITCODE -eq 0) } catch { $dockerReady = $false }
if (-not $dockerReady) {
    $desktop = Join-Path $env:ProgramFiles 'Docker\Docker\Docker Desktop.exe'
    if (Test-Path $desktop) { Start-Process $desktop }
    Write-Host 'Жду запуска Docker Desktop (до 3 минут)...'
    for ($i = 0; $i -lt 60; $i++) {
        Start-Sleep -Seconds 3
        try { & $dockerExe info *> $null; if ($LASTEXITCODE -eq 0) { $dockerReady = $true; break } } catch { }
    }
}
if (-not $dockerReady) { throw 'Docker Desktop не запустился. Откройте Docker Desktop, дождитесь статуса Running и повторите запуск BAT.' }

$composeMode = ''
$composeExe = ''
try { & $dockerExe compose version *> $null; if ($LASTEXITCODE -eq 0) { $composeMode = 'plugin' } } catch { }
if (-not $composeMode) {
    $composeCommand = Get-Command docker-compose.exe -ErrorAction SilentlyContinue
    if (-not $composeCommand) { $composeCommand = Get-Command docker-compose -ErrorAction SilentlyContinue }
    if ($composeCommand) {
        $composeExe = $composeCommand.Source
        try { & $composeExe version *> $null; if ($LASTEXITCODE -eq 0) { $composeMode = 'standalone' } } catch { }
    }
}
if (-not $composeMode) {
    throw 'Docker запущен, но Docker Compose не найден. Обновите Docker Desktop, затем закройте и снова откройте его. Нужна поддержка команды docker compose.'
}
Write-Host "Compose mode: $composeMode"

$installDir = Join-Path $env:LOCALAPPDATA 'CourierkaServer'
New-Item -ItemType Directory -Path $installDir -Force | Out-Null

$requiredBackendFiles = @('Dockerfile', 'compose.yaml', 'package.json', 'package-lock.json', 'server.js')
$projectRoot = $env:COURIERKA_PROJECT_ROOT
if ([string]::IsNullOrWhiteSpace($projectRoot)) { $projectRoot = Split-Path -Parent $PSScriptRoot }
$localBackend = Join-Path $projectRoot 'backend'
$localBackendReady = Test-Path (Join-Path $localBackend 'compose.yaml')
foreach ($file in $requiredBackendFiles) {
    if (-not (Test-Path (Join-Path $localBackend $file))) { $localBackendReady = $false; break }
}

if ($localBackendReady) {
    Write-Step 'Использую backend из папки рядом с BAT-файлом'
    $backendSource = $localBackend
} else {
    Write-Step 'Локальная папка backend не найдена; скачиваю нужные файлы'
    $backendSource = Join-Path $env:TEMP ('CourierkaBackend-' + [Guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $backendSource -Force | Out-Null
    $rawBase = 'https://raw.githubusercontent.com/AlfaGideon/MessengerForCouriers/main/backend'
    foreach ($file in $requiredBackendFiles) {
        $fileUrl = "$rawBase/$file"
        try {
            Invoke-WebRequest -UseBasicParsing -Uri $fileUrl -OutFile (Join-Path $backendSource $file) -TimeoutSec 90
        } catch {
            throw "Не удалось скачать backend-файл $fileUrl. Проверьте доступ к GitHub и повторите запуск. Подробности: $($_.Exception.Message)"
        }
    }
}

foreach ($file in $requiredBackendFiles) {
    Copy-Item -Path (Join-Path $backendSource $file) -Destination $installDir -Force
}
if (-not $localBackendReady) { Remove-Item $backendSource -Recurse -Force -ErrorAction SilentlyContinue }

$envFile = Join-Path $installDir '.env'
if (-not (Test-Path $envFile)) {
    Write-Step 'Создаю секрет и локальную конфигурацию'
    $bytes = New-Object byte[] 48
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($bytes)
    $rng.Dispose()
    $secret = ([BitConverter]::ToString($bytes)).Replace('-', '').ToLowerInvariant()
    $config = "JWT_SECRET=$secret`r`nPORT=3000`r`nHOST=0.0.0.0`r`nDATA_DIR=/app/data`r`nCORS_ORIGIN=*`r`n"
    [System.IO.File]::WriteAllText($envFile, $config, [System.Text.UTF8Encoding]::new($false))
}

Write-Step 'Собираю и запускаю backend'
Push-Location $installDir
try {
    if ($composeMode -eq 'plugin') {
        & $dockerExe compose up -d --build
    } else {
        & $composeExe -f compose.yaml up -d --build
    }
    if ($LASTEXITCODE -ne 0) { throw 'Docker Compose не смог собрать или запустить backend.' }
} finally { Pop-Location }

$backendOrigin = $env:COURIERKA_BACKEND_ORIGIN
if ([string]::IsNullOrWhiteSpace($backendOrigin)) { $backendOrigin = 'http://127.0.0.1:3000' }
$backendOrigin = $backendOrigin.TrimEnd('/')
$healthUrl = "$backendOrigin/api/health"
$healthy = $false
for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 2
    try {
        $health = Invoke-RestMethod -Uri $healthUrl -TimeoutSec 4
        if ($health.ok) { $healthy = $true; break }
    } catch { }
}
if (-not $healthy) { throw "Backend не ответил на $healthUrl. Проверьте статус контейнера в Docker Desktop." }
Write-Host "Backend отвечает: $healthUrl" -ForegroundColor Green

Write-Step 'Готовлю Cloudflare Tunnel'
$cloudflared = Join-Path $installDir 'cloudflared.exe'
if (-not (Test-Path $cloudflared)) {
    $cloudflaredUrl = 'https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe'
    Invoke-WebRequest -UseBasicParsing -Uri $cloudflaredUrl -OutFile $cloudflared
}

$pidFile = Join-Path $installDir 'cloudflared.pid'
if (Test-Path $pidFile) {
    $oldPid = 0
    [int]::TryParse((Get-Content $pidFile -Raw).Trim(), [ref]$oldPid) | Out-Null
    if ($oldPid -gt 0) { Stop-Process -Id $oldPid -Force -ErrorAction SilentlyContinue }
    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
}
$stdout = Join-Path $installDir 'cloudflared.out.log'
$stderr = Join-Path $installDir 'cloudflared.err.log'
Remove-Item $stdout, $stderr -Force -ErrorAction SilentlyContinue
$tunnel = Start-Process -FilePath $cloudflared -ArgumentList @('tunnel', '--url', $backendOrigin, '--no-autoupdate') -WorkingDirectory $installDir -RedirectStandardOutput $stdout -RedirectStandardError $stderr -WindowStyle Hidden -PassThru
Set-Content -Path $pidFile -Value $tunnel.Id -Encoding Ascii

$publicUrl = ''
for ($i = 0; $i -lt 60; $i++) {
    Start-Sleep -Seconds 1
    $logs = ''
    if (Test-Path $stdout) { $logs += Get-Content $stdout -Raw -ErrorAction SilentlyContinue }
    if (Test-Path $stderr) { $logs += Get-Content $stderr -Raw -ErrorAction SilentlyContinue }
    $match = [Regex]::Match($logs, 'https://[a-z0-9-]+\.trycloudflare\.com')
    if ($match.Success) { $publicUrl = $match.Value; break }
    if ($tunnel.HasExited) { break }
}
if (-not $publicUrl) {
    $logs = ''
    if (Test-Path $stdout) { $logs += Get-Content $stdout -Raw -ErrorAction SilentlyContinue }
    if (Test-Path $stderr) { $logs += Get-Content $stderr -Raw -ErrorAction SilentlyContinue }
    Write-Host $logs
    throw 'Cloudflare Tunnel не выдал адрес. Проверьте подключение к интернету и повторите запуск.'
}

try { Set-Clipboard -Value $publicUrl } catch { }
Write-Step 'Готово — сервер доступен из интернета'
Write-Host "Публичный адрес: $publicUrl" -ForegroundColor Green
Write-Host "Проверка:        $publicUrl/api/health"
Write-Host ''
Write-Host 'На КАЖДОМ телефоне: Курьерка → Ещё → Сервер сообщества → вставить этот адрес → Сохранить.' -ForegroundColor Yellow
Write-Host 'Затем войдите или зарегистрируйтесь во вкладке Чаты. Все пользователи должны указать один и тот же адрес.'
Write-Host ''
Write-Host 'Адрес уже скопирован в буфер обмена. Открываю проверку сервера...'
Start-Process "$publicUrl/api/health"
Write-Host ''
Write-Host 'ВАЖНО: это временный тестовый адрес. Не закрывайте компьютер и не выключайте интернет.' -ForegroundColor Yellow
Write-Host 'После перезапуска BAT выдаст новый адрес; его нужно заново указать на телефонах.'
Write-Host "Файлы и база сервера хранятся здесь: $installDir"
