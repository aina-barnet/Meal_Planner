@echo off
rem Loads a copy made with "Guardar dades.bat" into the app, replacing the data it has now.
rem   Drag a .dump file onto this file, or double-click it to load the most recent copy in
rem   copies-de-seguretat\. The current data is saved first (abans-de-recuperar-<date>.dump).
rem A copy from an older version of the app is fine: on start, the API upgrades it (Flyway).
chcp 65001 >nul
setlocal
title Planificador d'àpats - Recuperant les dades
cd /d "%~dp0"

echo.
echo   ==========================================
echo     Planificador d'àpats - Recuperar dades
echo   ==========================================
echo.

set "FILE=%~1"
if defined FILE goto :have_file
for /f "delims=" %%f in ('dir /b /o-d "copies-de-seguretat\*.dump" 2^>nul') do (
    set "FILE=%CD%\copies-de-seguretat\%%f"
    goto :have_file
)
echo   No hi ha cap còpia a la carpeta copies-de-seguretat.
echo   Arrossega el fitxer .dump a sobre de "Recuperar dades.bat".
goto :fail

:have_file
if not exist "%FILE%" (
    echo   No es troba el fitxer: %FILE%
    goto :fail
)

docker info >nul 2>&1
if errorlevel 1 (
    echo   Docker Desktop no està en marxa. Engega l'aplicació amb Engegar.bat i torna-ho a provar.
    goto :fail
)

echo   Es carregaran les dades de:
echo     %FILE%
echo.
echo   Les dades que té ara l'aplicació se substituiran, però abans se'n guardarà una còpia.
set "ANSWER="
set /p "ANSWER=  Vols continuar? (S/N): "
if /i not "%ANSWER%"=="S" (
    echo   No s'ha canviat res.
    goto :fail
)
echo.

echo   [1/4] Guardant una còpia de les dades actuals...
call "%~dp0Guardar dades.bat" /abans
if errorlevel 1 goto :fail

echo   [2/4] Aturant l'aplicació...
docker compose stop api >nul 2>&1

echo   [3/4] Carregant les dades...
docker compose cp "%FILE%" db:/tmp/recupera.dump >nul 2>&1
if errorlevel 1 (
    echo   No s'ha pogut llegir el fitxer. No s'ha canviat res.
    goto :restart_fail
)
docker compose exec -T db dropdb -U mealplanner --force --if-exists mealplanner
docker compose exec -T db createdb -U mealplanner -O mealplanner mealplanner
docker compose exec -T db pg_restore -U mealplanner -d mealplanner --no-owner --no-privileges --exit-on-error /tmp/recupera.dump
if errorlevel 1 (
    echo.
    echo   El fitxer no s'ha pogut carregar. Torna a posar les dades d'abans amb la còpia
    echo   "abans-de-recuperar" més recent de la carpeta copies-de-seguretat.
    docker compose exec -T db rm -f /tmp/recupera.dump >nul 2>&1
    goto :restart_fail
)
docker compose exec -T db rm -f /tmp/recupera.dump >nul 2>&1

:restart
echo   [4/4] Engegant l'aplicació...
docker compose up -d >nul
if errorlevel 1 goto :restart_fail
echo.
echo   Fet! Les dades s'han recuperat. Obre http://localhost:3000
echo.
pause
exit /b 0

:restart_fail
docker compose up -d >nul 2>&1
:fail
echo.
pause
exit /b 1
