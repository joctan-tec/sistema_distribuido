import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

public class Master {

    public static String getIpNode() {
        try {
            // Inicializar el cliente de Kubernetes
            ApiClient client = Config.defaultClient();
            CoreV1Api api = new CoreV1Api(client);

            // Obtener la lista de pods
            V1PodList podList = api.listPodForAllNamespaces(
                    null, null, null, null, null, null, null, null, null, false);

            // Obtener los nombres de los pods y las IPs de los nodos
            List<String> podNames = podList.getItems().stream()
                    .map(V1Pod::getMetadata)
                    .map(metadata -> metadata.getName())
                    .collect(Collectors.toList());
            List<String> nodeIps = podList.getItems().stream()
                    .map(V1Pod::getStatus)
                    .map(status -> status.getPodIP())
                    .collect(Collectors.toList());

            // Validar que existe el pod llamado "node-pod"
            if (!podNames.contains("node-pod")) {
                return "El pod con nombre 'node-pod' no fue encontrado.";
            }

            // Obtener la IP del pod con nombre "node-pod"
            String nodeIp = nodeIps.get(podNames.indexOf("node-pod"));

            return nodeIp != null ? nodeIp : "No se encontró una IP para el pod 'node-pod'.";

        } catch (ApiException e) {
            return "Error al listar los pods. Detalles: " + e.getResponseBody();
        } catch (IOException e) {
            return "Error al inicializar el cliente de Kubernetes: " + e.getMessage();
        }
    }

    public static void main(String[] args) {
        // Ejecutar el método para listar los pods y obtener la IP del pod deseado
        String nodeIp = getIpNode();
        System.out.println("Resultado: " + nodeIp);

        // Verificar si se obtuvo una IP válida
        if (nodeIp == null || nodeIp.startsWith("Error")) {
            System.out.println("No se puede proceder con la consulta debido a un error.");
            return;
        }

        // Realizar una consulta HTTP a la IP del pod
        try {
            URL url = new URL("http://" + nodeIp + ":8081/test"); // Ajusta el puerto según sea necesario
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            System.out.println("Respuesta de la consulta al pod 'node-pod': Código de respuesta = " + responseCode);

            if (responseCode == 200) {
                System.out.println("Consulta exitosa al pod 'node-pod'.");
                // Mostrar la respuesta del pod
                System.out.println("Respuesta del pod 'node-pod':");
                connection.getInputStream().transferTo(System.out);

            } else {
                System.out.println("Consulta fallida. Código de respuesta: " + responseCode);
            }

        } catch (IOException e) {
            System.out.println("Error al realizar la consulta HTTP: " + e.getMessage());
        }
    }
}