#!/bin/bash

# Programa que genera un carnet único y lo guarda en un archivo.
# El carnet está compuesto por el año actual y un número aleatorio de 6 dígitos.
# Autor: Joctan Porras y Brenda Badilla

# Variables
timestamp=$(date +"%Y-%m-%d %T")
anno=$(date +%Y)
archivo="/home/joctan04/Documentos/TEC/Semestre_VII/Sistemas_Operativos/Proyecto02/data/carnets.txt"
logs="/home/joctan04/Documentos/TEC/Semestre_VII/Sistemas_Operativos/Proyecto02/logs/carnets.log"

# Crear el archivo si no existe
mkdir -p "$(dirname "$archivo")"
touch "$archivo"

# Crear el archivo de logs si no existe
mkdir -p "$(dirname "$logs")"
touch "$logs"

# Funciones
function generarCarnet {
    local nuevo_carnet
    while :; do
        nuevo_carnet="${anno}$(shuf -i 100000-999999 -n 1)"
        if ! grep -Fxq "$nuevo_carnet" "$archivo"; then
            echo "$nuevo_carnet"
            return
        fi
    done
}

# Main
carnet=$(generarCarnet)
echo $carnet >> $archivo
echo "Carnet generado: $carnet"
echo "[$timestamp] Carnet generado: $carnet" >> $logs

exit 0