@echo off
rem Saves a copy of all the family's data into copies-de-seguretat\ (a pg_dump of the database).
rem The file can be copied to another computer and loaded there with "Recuperar dades.bat".
rem   Guardar dades.bat          -> copies-de-seguretat\planificador-<date>.dump, then waits for a key
rem   Guardar dades.bat /auto    -> copies-de-seguretat\auto-<date>.dump, keeps the last 10, no pause
rem   Guardar dades.bat /abans   -> copies-de-seguretat\abans-de-recuperar-<date>.dump, no pause
chcp 65001 >nul
setlocal
cd /d "%~dp0"

set "PREFIX=planificador"
set "QUIET="
if /i "%~1"=="/auto" (set "PREFIX=auto" & set "QUIET=1")
if /i "%~1"=="/abans" (set "PREFIX=abans-de-recuperar" & set "QUIET=1")

if not defined QUIET (
    title Planificador d'àpats - Guardant les dades
    echo.
    echo   ==========================================
    echo     Planificador d'àpats - Guardar dades
    echo   ==========================================
    echo.
)

docker info >nul 2>&1
if errorlevel 1 (
    echo   Docker Desktop no està en marxa. Engega l'aplicació amb Engegar.bat i torna-ho a provar.
    goto :fail
)

rem Only the database is needed, so this also works while the rest of the app is stopped.
docker compose up -d --wait db >nul 2>&1
if errorlevel 1 (
    echo   No s'ha pogut engegar la base de dades.
    goto :fail
)

for /f %%i in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd-HHmmss"') do set "STAMP=%%i"
if not exist "copies-de-seguretat" mkdir "copies-de-seguretat"
set "FILE=copies-de-seguretat\%PREFIX%-%STAMP%.dump"

docker compose exec -T db pg_dump -U mealplanner -d mealplanner -Fc -f /tmp/copia.dump
if errorlevel 1 (
    echo   No s'han pogut llegir les dades de la base de dades.
    goto :fail
)
docker compose cp db:/tmp/copia.dump "%FILE%" >nul 2>&1
if errorlevel 1 (
    echo   No s'ha pogut desar el fitxer %FILE%
    goto :fail
)
docker compose exec -T db rm -f /tmp/copia.dump >nul 2>&1

echo   Dades guardades a: %CD%\%FILE%

if /i "%PREFIX%"=="auto" (
    for /f "skip=10 delims=" %%f in ('dir /b /o-n "copies-de-seguretat\auto-*.dump" 2^>nul') do del "copies-de-seguretat\%%f"
)

if not defined QUIET (
    echo.
    echo   Per passar les dades a un altre ordinador, copia aquest fitxer i, a l'altre ordinador,
    echo   arrossega'l a sobre de "Recuperar dades.bat".
    echo.
    pause
)
exit /b 0

:fail
if not defined QUIET (
    echo.
    pause
)
exit /b 1
