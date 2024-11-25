#!/bin/bash
# Variables
current_dir=$(realpath "$0")
current_dir=$(dirname "$current_dir")
node_implementation_dir=$(dirname "$current_dir")
n_nodes=$1

# Verificar que el input sea un número entero mayor a 0
if ! [[ $n_nodes =~ ^[1-9][0-9]*$ ]]; then
    echo "Se recibió: $n_nodes"
    echo "El input debe ser un número entero mayor a 0"
    exit 1
fi

# Crear los nodos
for i in $(seq 1 $n_nodes); do
    kubectl apply -f <(sed "s/PLACEHOLDER/$(date +%Y%m%d%H%M%S%3N)/" $node_implementation_dir/k8s/generate-pods-job.yaml)
done

exit 0