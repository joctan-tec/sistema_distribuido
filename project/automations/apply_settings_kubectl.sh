#! /bin/bash

# Variables
current_dir=$(realpath "$0")
current_dir=$(dirname "$current_dir")
project_root_dir=$(dirname "$current_dir")

# Aplicar configuraciones de Kubectl
kubectl apply -f $project_root_dir/master_implementation/k8s/service-account.yaml
kubectl apply -f $project_root_dir/master_implementation/k8s/cluster-role.yaml
kubectl apply -f $project_root_dir/master_implementation/k8s/cluster-role-binding.yaml

# Master
kubectl apply -f $project_root_dir/master_implementation/k8s/master-service.yaml
kubectl apply -f $project_root_dir/master_implementation/k8s/master-deployment.yaml


# Node
kubectl apply -f $project_root_dir/node_implementation/k8s/node-pod.yaml