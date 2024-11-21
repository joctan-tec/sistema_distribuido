@echo off

REM Variables
SET current_dir=%~dp0
SET current_dir=%current_dir:~0,-1%
FOR %%A IN ("%current_dir%") DO SET project_root_dir=%%~dpA
SET project_root_dir=%project_root_dir:~0,-1%

REM Aplicar configuraciones de Kubectl

kubectl apply -f "%project_root_dir%\master_implementation\k8s\service-account-master.yaml"
kubectl apply -f "%project_root_dir%\master_implementation\k8s\cluster-role-master.yaml"
kubectl apply -f "%project_root_dir%\master_implementation\k8s\cluster-role-binding-master.yaml"

REM Configuración del master
kubectl apply -f "%project_root_dir%\master_implementation\k8s\master-service.yaml"
kubectl apply -f "%project_root_dir%\master_implementation\k8s\master-deployment.yaml"

kubectl apply -f "%project_root_dir%\node_implementation\k8s\service-account-node.yaml"
kubectl apply -f "%project_root_dir%\node_implementation\k8s\cluster-role-node.yaml"
kubectl apply -f "%project_root_dir%\node_implementation\k8s\cluster-role-binding-node.yaml"

exit /b 0
