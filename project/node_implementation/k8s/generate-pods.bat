@echo off

REM Generar la marca de tiempo actual
for /f %%i in ('powershell -Command "Get-Date -Format yyyyMMddHHmmssfff"') do set TIMESTAMP=%%i

REM Crear un archivo temporal con el contenido actualizado
set TEMP_FILE=temp-job.yaml
powershell -Command "(Get-Content 'C:\Users\Brend\OneDrive\Documents\GitHub\sistema_distribuido\project\node_implementation\k8s\generate-pods-job.yaml') -replace 'PLACEHOLDER', '%TIMESTAMP%' | Set-Content '%TEMP_FILE%'"

REM Aplicar el archivo modificado con kubectl
kubectl apply -f %TEMP_FILE%

REM Eliminar el archivo temporal
del %TEMP_FILE%

exit /b 0
