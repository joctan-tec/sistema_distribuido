#! /bin/bash

# Variables
current_dir=$(realpath "$0")
current_dir=$(dirname "$current_dir")
project_root_dir=$(dirname "$current_dir")

# Crear las imágenes de Docker
# Construir la imagen para los nodos
docker build -t joctan04/ds-node:latest -f $project_root_dir/docker/Dockerfile.node .

# Construir la imagen para el master
docker build -t joctan04/ds-master:latest -f $project_root_dir/docker/Dockerfile.master .