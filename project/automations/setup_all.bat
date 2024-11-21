@echo off

REM Variables
SET current_dir=%~dp0
SET current_dir=%current_dir:~0,-1%
FOR %%A IN ("%current_dir%") DO SET project_root_dir=%%~dpA
SET project_root_dir=%project_root_dir:~0,-1%

REM Cambia al directorio raíz del proyecto
cd /d "%project_root_dir%"

REM Limpia y construye los jars con Maven
mvn clean package

REM Ejecuta todos los scripts de configuración
"./%current_dir%\create_docker_images.bat"

REM Espera hasta que las imágenes de Docker estén listas
:wait_for_image
docker images -q brendabbr2/ds-master:latest >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    timeout /t 1 >nul
    GOTO wait_for_image
)

REM Aplica configuraciones con kubectl
"./%current_dir%\apply_settings_kubectl.bat"

exit /b 0
