import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

public class Master {



public static HashMap<String, String> getNodes() {
    try {
        // Inicializar el cliente de Kubernetes
        ApiClient client = Config.defaultClient();
        CoreV1Api api = new CoreV1Api(client);
        HashMap<String, String> nodes = new HashMap<>();
        Pattern pattern = Pattern.compile("^node-job.*");

        // Obtener la lista de pods
        V1PodList podList = api.listPodForAllNamespaces(
                null, null, null, null, null, null, null, null, null, false);

        // Filtrar y mapear los pods que cumplen con el patrón
        List<V1Pod> filteredPods = podList.getItems().stream()
                .filter(pod -> pattern.matcher(pod.getMetadata().getName()).matches())
                .collect(Collectors.toList());

        // Imprimir los nombres de los pods y las IPs que cumplen con el patrón
        for (V1Pod pod : filteredPods) {
            String podName = pod.getMetadata().getName();
            String podIp = pod.getStatus().getPodIP();
            //System.out.println("Pod: " + podName + ", IP: " + podIp);
            nodes.put(podName, podIp);
        }

        return nodes;

    } catch (ApiException e) {
        System.out.println("Error al listar los pods: " + e.getResponseBody());
        return null;
    } catch (IOException e) {
        System.out.println("Error al obtener la configuración del cliente: " + e.getMessage());
        return null;
    }
}

    public static void main(String[] args) {
        // Ejecutar el método para listar los pods y obtener la IP del pod deseado
        HashMap<String, String> nodes = getNodes();
        if (nodes == null) {
            System.out.println("No se encontró el pod 'node-job'.");
            return;
        }

        // Hacer una petición HTTP a cada nodo
        for (String nodeName : nodes.keySet()) {
            String nodeIp = nodes.get(nodeName);
            try {
                URL url = new URL("http://" + nodeIp + ":8081/test");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");

                int status = con.getResponseCode();
                if (status == 200) {
                    // Leer la respuesta del nodo
                    System.out.println("\n--------------------------------------------\n");
                    System.out.println("GET " + url + " " + status);
                    System.out.println("Respuesta del nodo " + nodeName + ":");
                    con.getInputStream().transferTo(System.out);
                    System.out.println("\n--------------------------------------------\n");

                } else {
                    System.out.println("GET " + url + " " + status);
                }
            } catch (IOException e) {
                System.out.println("Error al hacer la petición a " + nodeIp + ": " + e.getMessage());
            }
        }

        
    }
}