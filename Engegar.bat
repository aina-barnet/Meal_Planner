@echo off
rem Starts the meal planner with a double click: Docker Desktop, the containers, and the browser.
chcp 65001 >nul
title Planificador d'àpats - Engegant
cd /d "%~dp0"

set "APP_URL=http://localhost:3000"
rem Goes through nginx to the API, so it only answers once the whole stack is up.
set "READY_URL=http://localhost:3000/api/ingredients"

echo.
echo   ==========================================
echo     Planificador d'àpats
echo   ==========================================
echo.

where docker >nul 2>&1
if errorlevel 1 (
    echo   Aquest ordinador encara no està preparat.
    echo   Fes doble clic a Instal·lar.bat, reinicia l'ordinador i torna a provar-ho.
    goto :fail
)

docker info >nul 2>&1
if not errorlevel 1 goto :docker_ready

echo   [1/3] Engegant Docker Desktop...
if exist "%ProgramFiles%\Docker\Docker\Docker Desktop.exe" (
    start "" "%ProgramFiles%\Docker\Docker\Docker Desktop.exe"
) else (
    docker desktop start >nul 2>&1
)

rem Docker Desktop can take a few minutes to boot on a cold start. The limit is wall-clock time
rem because a single "docker info" can hang for a minute while the engine is still starting.
powershell -NoProfile -Command "$end = (Get-Date).AddMinutes(5); while ((Get-Date) -lt $end) { docker info *> $null; if ($LASTEXITCODE -eq 0) { exit 0 }; Start-Sleep -Seconds 3 }; exit 1"
if errorlevel 1 (
    echo.
    echo   Docker Desktop no ha arrencat a temps.
    echo   Mira la finestra de Docker Desktop: potser demana alguna cosa.
    echo   Quan digui que està en marxa, torna a fer doble clic a Engegar.bat
    echo   Si és la primera vegada en aquest ordinador, executa abans Instal·lar.bat
    goto :fail
)

:docker_ready
echo   [1/3] Docker Desktop en marxa.

rem Safety copy before every start: a new version of the app may upgrade the database.
rem Only when there is already data, i.e. not on the very first start on this computer.
docker volume inspect meal-planner_pgdata >nul 2>&1
if not errorlevel 1 (
    echo   Guardant una còpia de seguretat de les dades...
    call "%~dp0Guardar dades.bat" /auto >nul
    if errorlevel 1 echo   Avís: no s'ha pogut fer la còpia de seguretat automàtica.
)

echo   [2/3] Engegant l'aplicació (la primera vegada pot trigar uns minuts)...
docker compose up -d --build
if errorlevel 1 (
    echo.
    echo   No s'ha pogut engegar l'aplicació. Revisa el missatge d'error de dalt.
    goto :fail
)

echo   [3/3] Esperant que l'aplicació estigui a punt...
powershell -NoProfile -Command "$end = (Get-Date).AddMinutes(3); while ((Get-Date) -lt $end) { try { Invoke-WebRequest -UseBasicParsing -TimeoutSec 5 '%READY_URL%' | Out-Null; exit 0 } catch { Start-Sleep -Seconds 2 } }; exit 1"
if errorlevel 1 (
    echo.
    echo   L'aplicació no respon. Prova d'aturar-la amb Aturar.bat i torna-la a engegar.
    goto :fail
)

echo.
echo   Llest! S'obre l'aplicació al navegador: %APP_URL%
echo   Per apagar-la, fes doble clic a Aturar.bat
start "" "%APP_URL%"
ping -n 6 127.0.0.1 >nul
exit /b 0

:fail
echo.
pause
exit /b 1
