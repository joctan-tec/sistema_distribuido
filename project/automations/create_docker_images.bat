@echo off

:: Variables
set "current_dir=%~dp0"
set "project_root_dir=C:\Users\Brend\OneDrive\Documents\GitHub\sistema_distribuido\project"

:: Crear las imágenes de Docker

:: Construir la imagen para el master
cd "%project_root_dir%\master_implementation"
docker build -t brendabbr2/ds-master:latest -f docker\Dockerfile.master .
cd "%project_root_dir%"

:: Construir la imagen para el node
cd "%project_root_dir%\node_implementation"
docker build -t brendabbr2/ds-node:latest -f docker\Dockerfile.node .
cd "%project_root_dir%"

:: Construir la imagen para el data_storage
cd "%project_root_dir%\data_storage_implementation"
docker build -t brendabbr2/ds-data-storage:latest -f docker\Dockerfile.data .
cd "%project_root_dir%"

:: Hacer push de las imágenes
docker push brendabbr2/ds-master:latest
docker push brendabbr2/ds-node:latest
docker push brendabbr2/ds-data-storage:latest

exit /b 0
