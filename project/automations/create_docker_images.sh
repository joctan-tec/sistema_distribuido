#! /bin/bash

# Variables
current_dir=$(realpath "$0")
current_dir=$(dirname "$current_dir")
project_root_dir=$(dirname "$current_dir")

# Crear las imágenes de Docker
# Construir la imagen para el master
cd master_implementation
docker build -t joctan04/ds-master:latest -f docker/Dockerfile.master .
cd ..

# Construir la imagen para el master
cd node_implementation
docker build -t joctan04/ds-node:latest -f docker/Dockerfile.node .
cd ..


# Hacer push de las imágenes
docker push joctan04/ds-master:latest
docker push joctan04/ds-node:latest