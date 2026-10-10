@echo off
rem One-time setup of a Windows PC so Engegar.bat can run: WSL 2 and Docker Desktop.
rem The app itself (images, containers, database) is created by Engegar.bat on first start.
chcp 65001 >nul
cd /d "%~dp0"

rem Needs administrator rights. Re-launch elevated, passing the real user so we can add them to docker-users.
net session >nul 2>&1
if errorlevel 1 (
    powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -ArgumentList '\"%USERDOMAIN%\%USERNAME%\"' -Verb RunAs"
    exit /b 0
)
set "TARGET_USER=%~1"
if "%TARGET_USER%"=="" set "TARGET_USER=%USERDOMAIN%\%USERNAME%"

title Planificador d'àpats - Instal·lació
set "NEEDS_RESTART=0"

echo.
echo   ==========================================
echo     Planificador d'àpats - Instal·lació
echo   ==========================================
echo.
echo   Aquest programa prepara l'ordinador perquè l'aplicació funcioni.
echo   Només cal fer-ho un cop. Pot trigar una estona; no tanquis la finestra.
echo.

rem Windows features that Docker Desktop runs on. 3010 means "done, restart required".
echo   [1/3] Activant les funcions de Windows necessàries...
dism /online /enable-feature /featurename:Microsoft-Windows-Subsystem-Linux /all /norestart /quiet
if errorlevel 3010 if not errorlevel 3011 set "NEEDS_RESTART=1"
dism /online /enable-feature /featurename:VirtualMachinePlatform /all /norestart /quiet
if errorlevel 3010 if not errorlevel 3011 set "NEEDS_RESTART=1"
wsl --update >nul 2>&1
echo         Fet.

echo   [2/3] Instal·lant Docker Desktop...
if exist "%ProgramFiles%\Docker\Docker\Docker Desktop.exe" (
    echo         Ja estava instal·lat.
    goto :docker_installed
)
where winget >nul 2>&1
if errorlevel 1 (
    echo.
    echo   No s'ha pogut instal·lar automàticament.
    echo   S'obrirà la pàgina de Docker Desktop: descarrega'l, instal·la'l i torna a executar aquest programa.
    start "" "https://www.docker.com/products/docker-desktop/"
    goto :fail
)
winget install -e --id Docker.DockerDesktop --accept-package-agreements --accept-source-agreements
if not exist "%ProgramFiles%\Docker\Docker\Docker Desktop.exe" (
    echo.
    echo   La instal·lació de Docker Desktop ha fallat. Revisa el missatge d'error de dalt.
    goto :fail
)
set "NEEDS_RESTART=1"
echo         Fet.

:docker_installed
rem Docker Desktop only lets members of docker-users talk to the engine.
net localgroup docker-users "%TARGET_USER%" /add >nul 2>&1

echo   [3/3] Comprovant la virtualització del processador...
powershell -NoProfile -Command "$cs = Get-CimInstance Win32_ComputerSystem; $cpu = Get-CimInstance Win32_Processor | Select-Object -First 1; if ($cs.HypervisorPresent -or $cpu.VirtualizationFirmwareEnabled) { exit 0 } else { exit 1 }"
if errorlevel 1 (
    echo.
    echo   ATENCIÓ: la virtualització sembla desactivada a la BIOS de l'ordinador.
    echo   Sense ella Docker no pot funcionar. Cal activar "Intel VT-x" o "AMD-V / SVM"
    echo   a la configuració de la BIOS, normalment prement F1, F2 o Supr en engegar.
    echo   Més informació: https://aka.ms/enablevirtualization
    echo.
) else (
    echo         Correcte.
)

echo.
echo   ==========================================
echo   Instal·lació acabada.
echo.
if "%NEEDS_RESTART%"=="1" (
    echo   Cal REINICIAR l'ordinador perquè els canvis tinguin efecte.
    echo   Després del reinici, fes doble clic a Engegar.bat.
    echo   La primera vegada Docker Desktop pot demanar que acceptis les condicions: accepta-les.
    echo.
    choice /c SN /m "  Vols reiniciar ara"
    if not errorlevel 2 shutdown /r /t 5
    exit /b 0
)
echo   Ja pots fer doble clic a Engegar.bat.
echo.
pause
exit /b 0

:fail
echo.
pause
exit /b 1
