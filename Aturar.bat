@echo off
rem Stops the meal planner. The family's data stays in the pgdata volume (never use "down -v" here).
chcp 65001 >nul
title Planificador d'àpats - Aturant
cd /d "%~dp0"

echo.
echo   ==========================================
echo     Planificador d'àpats
echo   ==========================================
echo.

docker info >nul 2>&1
if errorlevel 1 (
    echo   Docker Desktop no està en marxa, per tant l'aplicació ja està aturada.
    goto :done
)

echo   Aturant l'aplicació...
docker compose down
if errorlevel 1 (
    echo.
    echo   No s'ha pogut aturar l'aplicació. Revisa el missatge d'error de dalt.
    echo.
    pause
    exit /b 1
)

echo.
echo   Aplicació aturada. Les dades es conserven per a la propera vegada.

:done
ping -n 5 127.0.0.1 >nul
exit /b 0
