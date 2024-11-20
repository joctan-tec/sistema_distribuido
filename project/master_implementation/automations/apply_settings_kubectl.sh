#! /bin/bash

# Variables
current_dir=$(realpath "$0")
current_dir=$(dirname "$current_dir")
project_root_dir=$(dirname "$current_dir")

kubectl apply -f $project_root_dir/k8s/master-deployment.yaml
kubectl apply -f $project_root_dir/k8s/master-service.yaml