$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

function Write-Step([string]$Message) {
    Write-Host "`n== $Message" -ForegroundColor Cyan
}

Write-Host 'Курьерка — запуск backend и временного глобального адреса' -ForegroundColor Yellow

$docker = Get-Command docker.exe -ErrorAction SilentlyContinue
if (-not $docker) {
    Write-Host 'Сначала установите Docker Desktop: https://www.docker.com/products/docker-desktop/' -ForegroundColor Red
    Start-Process 'https://www.docker.com/products/docker-desktop/'
    throw 'Установите Docker Desktop, перезагрузите компьютер при запросе и запустите этот BAT-файл снова.'
}

Write-Step 'Проверяю Docker'
$dockerReady = $false
try { & docker.exe info *> $null; $dockerReady = ($LASTEXITCODE -eq 0) } catch { $dockerReady = $false }
if (-not $dockerReady) {
    $desktop = Join-Path $env:ProgramFiles 'Docker\Docker\Docker Desktop.exe'
    if (Test-Path $desktop) { Start-Process $desktop }
    Write-Host 'Жду запуска Docker Desktop (до 3 минут)...'
    for ($i = 0; $i -lt 60; $i++) {
        Start-Sleep -Seconds 3
        try { & docker.exe info *> $null; if ($LASTEXITCODE -eq 0) { $dockerReady = $true; break } } catch { }
    }
}
if (-not $dockerReady) { throw 'Docker Desktop не запустился. Откройте Docker Desktop, дождитесь статуса Running и повторите запуск BAT.' }

$installDir = Join-Path $env:LOCALAPPDATA 'CourierkaServer'
New-Item -ItemType Directory -Path $installDir -Force | Out-Null

Write-Step 'Скачиваю backend из репозитория'
$tempDir = Join-Path $env:TEMP ('CourierkaSetup-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $tempDir -Force | Out-Null
$zipFile = Join-Path $tempDir 'project.zip'
$zipUrl = 'https://github.com/AlfaGideon/MessengerForCouriers/archive/refs/heads/arena/2002ef57-messengerforcouriers.zip'
Invoke-WebRequest -UseBasicParsing -Uri $zipUrl -OutFile $zipFile
Expand-Archive -Path $zipFile -DestinationPath $tempDir -Force
$backendSource = Get-ChildItem -Path $tempDir -Directory | ForEach-Object { Join-Path $_.FullName 'backend' } | Where-Object { Test-Path (Join-Path $_ 'compose.yaml') } | Select-Object -First 1
if (-not $backendSource) { throw 'В архиве не найдена папка backend.' }
Copy-Item -Path (Join-Path $backendSource '*') -Destination $installDir -Recurse -Force
Remove-Item $tempDir -Recurse -Force -ErrorAction SilentlyContinue

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
    & docker.exe compose up -d --build
    if ($LASTEXITCODE -ne 0) { throw 'Docker Compose не смог собрать или запустить backend.' }
} finally { Pop-Location }

$healthy = $false
for ($i = 0; $i -lt 30; $i++) {
    Start-Sleep -Seconds 2
    try {
        $health = Invoke-RestMethod -Uri 'http://localhost:3000/api/health' -TimeoutSec 4
        if ($health.ok) { $healthy = $true; break }
    } catch { }
}
if (-not $healthy) { throw 'Backend не ответил на http://localhost:3000/api/health. Проверьте Docker Desktop.' }
Write-Host 'Backend отвечает: http://localhost:3000/api/health' -ForegroundColor Green

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
$tunnel = Start-Process -FilePath $cloudflared -ArgumentList @('tunnel', '--url', 'http://localhost:3000', '--no-autoupdate') -WorkingDirectory $installDir -RedirectStandardOutput $stdout -RedirectStandardError $stderr -WindowStyle Hidden -PassThru
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
