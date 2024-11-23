@echo off

:: Variables
set "current_dir=%~dp0"
set "project_root_dir=%current_dir%"
set "project_root_dir=C:\Users\Brend\OneDrive\Documents\GitHub\sistema_distribuido\project\"

:: Aplicar configuraciones de Kubectl

kubectl apply -f "%project_root_dir%master_implementation\k8s\service-account-master.yaml"
kubectl apply -f "%project_root_dir%master_implementation\k8s\cluster-role-master.yaml"
kubectl apply -f "%project_root_dir%master_implementation\k8s\cluster-role-binding-master.yaml"

:: Master
kubectl apply -f "%project_root_dir%master_implementation\k8s\master-service.yaml"
kubectl apply -f "%project_root_dir%master_implementation\k8s\master-deployment.yaml"

kubectl apply -f "%project_root_dir%node_implementation\k8s\service-account-node.yaml"
kubectl apply -f "%project_root_dir%node_implementation\k8s\cluster-role-node.yaml"
kubectl apply -f "%project_root_dir%node_implementation\k8s\cluster-role-binding-node.yaml"

:: Data Storage
kubectl apply -f "%project_root_dir%data_storage_implementation\k8s\data-service.yaml"
kubectl apply -f "%project_root_dir%data_storage_implementation\k8s\data-deployment.yaml"

kubectl apply -f "%project_root_dir%data_storage_implementation\k8s\service-account-data.yaml"
kubectl apply -f "%project_root_dir%data_storage_implementation\k8s\cluster-role-data.yaml"
kubectl apply -f "%project_root_dir%data_storage_implementation\k8s\cluster-role-binding-data.yaml"
