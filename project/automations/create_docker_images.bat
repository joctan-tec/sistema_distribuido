@echo off

REM Variables
SET current_dir=%~dp0
SET current_dir=%current_dir:~0,-1%
FOR %%A IN ("%current_dir%") DO SET project_root_dir=%%~dpA
SET project_root_dir=%project_root_dir:~0,-1%

REM Crear las imágenes de Docker

REM Construir la imagen para el master
cd /d "%project_root_dir%\master_implementation"
docker build -t brendabbr2/ds-master:latest -f docker/Dockerfile.master .
cd /d "%project_root_dir%"

REM Construir la imagen para el nodo
cd /d "%project_root_dir%\node_implementation"
docker build -t brendabbr2/ds-node:latest -f docker/Dockerfile.node .
cd /d "%project_root_dir%"

REM Hacer push de las imágenes
docker push brendabbr2/ds-master:latest
docker push brendabbr2/ds-node:latest

exit /b 0
