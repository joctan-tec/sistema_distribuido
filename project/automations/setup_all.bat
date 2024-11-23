@echo off

:: Variables
set "current_dir=%~dp0"
set "project_root_dir=%current_dir%"
for %%a in ("%current_dir%..") do set "project_root_dir=%%a"

:: Crea los jar
cd /d "%project_root_dir%"
mvn clean package

:: Ejecuta todos los scripts de configuración
call "%current_dir%create_docker_images.bat"

:: Espera hasta que las imágenes de Docker estén listas
:wait_for_images
docker images -q brendabbr2/ds-node:latest > nul 2>&1
if errorlevel 1 (
    goto wait_for_images
)

docker images -q brendabbr2/ds-master:latest > nul 2>&1
if errorlevel 1 (
    goto wait_for_images
)

:: Ejecuta la configuración de kubectl
call "%current_dir%apply_settings_kubectl.bat"

exit /b 0
