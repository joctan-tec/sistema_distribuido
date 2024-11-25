#! /bin/bash

# Variables
current_dir=$(realpath "$0")
current_dir=$(dirname "$current_dir")
project_root_dir=$(dirname "$current_dir")
project_root_dir=$(dirname "$project_root_dir")
# Crea los jar
cd $project_root_dir

mvn clean package

cd data_implementation

# Ejecuta todos los scripts de configuración
sh $current_dir/create_docker_images.sh

# Espera hasta que las imágenes de Docker estén listas
while [ "$(docker images -q joctan04/ds-data:latest 2> /dev/null)" == "" ]; do
    sleep 1
done

sh $current_dir/apply_settings_kubectl.sh

exit 0
