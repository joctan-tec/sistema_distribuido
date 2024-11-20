#! /bin/bash
# Importar el certificado real si existe
if [ -f /var/run/secrets/kubernetes.io/serviceaccount/ca.crt ]; then
  keytool -importcert -file /var/run/secrets/kubernetes.io/serviceaccount/ca.crt -keystore /usr/local/openjdk-17/lib/security/cacerts -storepass changeit -noprompt -alias k8s-root-ca
fi

# Iniciar la aplicación
exec java -jar /app/master.jar