#! /bin/bash

# Variables
current_dir=$(realpath "$0")
current_dir=$(dirname "$current_dir")
project_root_dir=$(dirname "$current_dir")
# Crea los jar
cd $project_root_dir
mvn clean package

# Ejecuta todos los scripts de configuración
sh $current_dir/create_docker_images.sh

# Espera hasta que las imágenes de Docker estén listas
while ["$(docker images -q joctan04/ds-node:latest 2> /dev/null)" == "" ]||[ "$(docker images -q joctan04/ds-master:latest 2> /dev/null)" == "" ]; do
    sleep 1
done

sh $current_dir/apply_settings_kubectl.sh

exit 0
