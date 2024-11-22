#! /bin/bash

# Variables
current_dir=$(realpath "$0")
current_dir=$(dirname "$current_dir")
project_root_dir=$(dirname "$current_dir")

# Aplicar configuraciones de Kubectl
kubectl apply -f $project_root_dir/master_implementation/k8s/service-account-master.yaml
kubectl apply -f $project_root_dir/master_implementation/k8s/cluster-role-master.yaml
kubectl apply -f $project_root_dir/master_implementation/k8s/cluster-role-binding-master.yaml

# Master
kubectl apply -f $project_root_dir/master_implementation/k8s/master-service.yaml
kubectl apply -f $project_root_dir/master_implementation/k8s/master-deployment.yaml

kubectl apply -f $project_root_dir/node_implementation/k8s/service-account-node.yaml
kubectl apply -f $project_root_dir/node_implementation/k8s/cluster-role-node.yaml
kubectl apply -f $project_root_dir/node_implementation/k8s/cluster-role-binding-node.yaml